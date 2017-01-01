package com.coupon.common.session;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.GlobalLocationNumber;
import com.coupon.common.TimePeriod;
import com.coupon.common.entity.AwarderDetailEntity;
import com.coupon.common.entity.AwarderPointOfSaleEntity;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.RedemptionPeriodEntity;
import com.coupon.common.bean.AwarderDetailBean;
import com.coupon.common.bean.AwarderPointOfSaleBean;
import com.coupon.common.bean.RedemptionPeriodBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.AwarderDetailsRemote;
import com.coupon.common.utils.ExceptionHelper;
import com.coupon.common.utils.TimePeriodHelper;

@Remote
@LocalBean
@Stateless(name=AwarderDetailsRemote.NAME, mappedName=AwarderDetailsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class AwarderDetails implements AwarderDetailsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private AwarderPointOfSales aposEJB;
	@EJB private RedemptionPeriods rpEJB;
	@EJB private GlobalLocationNumbers glnEJB;
	@EJB private Offers oEJB;
	
	static final Logger logger = Logger.getLogger(AwarderDetails.class.getName());

	public AwarderDetails() {
	}

	public void validate(AwarderDetailEntity ade) throws DataValidationException {
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public AwarderDetailEntity findEntity(long adId) {

		AwarderDetailEntity adEnt = manager.find(AwarderDetailEntity.class, adId);		
	  	if (adEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("AwarderDetail resource with id '%d' not found", adId));
	  	}

		return adEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public AwarderDetailBean find(long adId) {

		AwarderDetailEntity adEnt = findEntity(adId);

		AwarderDetailBean adb = new AwarderDetailBean().init(adEnt);
		for (RedemptionPeriodBean rp : adb.getRedemptionPeriods()) {

			TimePeriod tp = TimePeriodHelper.toTimeZone(
					rp.getTimePeriod(),
					TimeZone.getTimeZone(adEnt.getOffer().getTimeZone()));
			rp.setTimePeriod(tp);
		}

		return adb;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public AwarderDetailEntity findEntityByCouponAndAwarderNumber(GlobalCouponNumber offerNumber,
										GlobalLocationNumber awarderNumber) {

		TypedQuery<AwarderDetailEntity> query = manager
				.createNamedQuery("AwarderDetailEntity.findByCouponAndAwarderNumber", AwarderDetailEntity.class)
				.setParameter("offerNumber", offerNumber)
				.setParameter("awarderNumber", awarderNumber);

		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
	  		throw new ResourceNotFoundException(
	  				String.format("AwarderDetail resource for GlobalCouponNumber '%s' and GlobalLocationNumber '%s' not found",
	  						offerNumber.toString(),
	  						awarderNumber.toString()));
		}
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AwarderDetailEntity createEntity(AwarderDetailBean ad) {

		AwarderDetailEntity adEnt = new AwarderDetailEntity();
		adEnt.setAwarderNumber(glnEJB.findEntityByNumber(ad.getAwarderNumber()));
		if (ad.getAwarderClearingAgentNumber() != null) {
			adEnt.setAwarderClearingAgentNumber(
					glnEJB.findEntityByNumber(ad.getAwarderClearingAgentNumber()));
		}

		Map<GlobalLocationNumber, AwarderPointOfSaleEntity> aposMap =
				new HashMap<GlobalLocationNumber, AwarderPointOfSaleEntity>();
		for (AwarderPointOfSaleBean apos : ad.getPointOfSales()) {
			AwarderPointOfSaleEntity aposEntity = aposEJB.createEntity(apos);
			aposEntity.setAwarderDetail(adEnt);
			aposMap.put(aposEntity.getStoreNumber(), aposEntity);
		}
		adEnt.setPointOfSalesMap(aposMap);

		Map<TimePeriod, RedemptionPeriodEntity> rpMap =
				new HashMap<TimePeriod, RedemptionPeriodEntity>();
		for (RedemptionPeriodBean rp : ad.getRedemptionPeriods()) {
			RedemptionPeriodEntity rpEntity = rpEJB.createEntity(rp);
			rpEntity.setAwarderDetail(adEnt);
			rpMap.put(rpEntity.getTimePeriod(), rpEntity);
		}
		adEnt.setRedemptionPeriodsMap(rpMap);

		return adEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AwarderDetailBean create(AwarderDetailBean ad) {

		if (ad.getParentId() == null) {
			throw new DataValidationException("offerId is required.");
		}

		OfferEntity oEnt = oEJB.findEntity(ad.getParentId());
		for (RedemptionPeriodBean rp : ad.getRedemptionPeriods()) {

			TimePeriod tp = TimePeriodHelper.toDefault(
					rp.getTimePeriod(),
					TimeZone.getTimeZone(oEnt.getTimeZone()));
			rp.setTimePeriod(tp);
		}
		AwarderDetailEntity adEnt = createEntity(ad);
		adEnt.setOffer(oEnt);

      	try {
      		manager.persist(adEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {
      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_OFFER_AWARDER_NUMBER")) {
	    			throw new ResourceConflictException(
	    	  				String.format("AwarderDetail with awarderNumber '%s' already exists",
	    	  						ad.getAwarderNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new AwarderDetailBean().init(adEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AwarderDetailEntity updateEntity(AwarderDetailBean ad) {

		AwarderDetailEntity adEntRef;

		try {

			adEntRef = manager.getReference(AwarderDetailEntity.class, ad.getId());
			adEntRef.setAwarderNumber(glnEJB.findEntityByNumber(ad.getAwarderNumber()));
			if (ad.getAwarderClearingAgentNumber() != null) {
				adEntRef.setAwarderClearingAgentNumber(
						glnEJB.findEntityByNumber(ad.getAwarderClearingAgentNumber()));
			}
			for (AwarderPointOfSaleBean apos : ad.getPointOfSales()) {
				AwarderPointOfSaleEntity aposEntity = aposEJB.updateEntity(apos);
				adEntRef.getPointOfSalesMap().put(aposEntity.getStoreNumber(), aposEntity);
			}
			for (RedemptionPeriodBean rp : ad.getRedemptionPeriods()) {
				RedemptionPeriodEntity rpEntity = rpEJB.updateEntity(rp);
				adEntRef.getRedemptionPeriodsMap().put(rpEntity.getTimePeriod(), rpEntity);
			}
			
			manager.merge(adEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AwarderDetail resource for id '%d' not found", ad.getId()));
		}
      	catch (PersistenceException ex) {
      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_OFFER_AWARDER_NUMBER")) {
	    			throw new ResourceConflictException(
	    	  				String.format("AwarderDetail with awarderNumber '%s' already exists",
	    	  						ad.getAwarderNumber().toString()));
      		}
      		throw ex;
      	}

		return adEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AwarderDetailBean update(AwarderDetailBean ad) {

		if (ad.getId() == null) {
			throw new DataValidationException("adId is required.");
		}

		OfferEntity oEnt = oEJB.findEntity(ad.getParentId());
		for (RedemptionPeriodBean rp : ad.getRedemptionPeriods()) {

			TimePeriod tp = TimePeriodHelper.toDefault(
					rp.getTimePeriod(),
					TimeZone.getTimeZone(oEnt.getTimeZone()));
			rp.setTimePeriod(tp);
		}
		AwarderDetailEntity adEntRef = updateEntity(ad);
		validate(adEntRef);

		return new AwarderDetailBean().init(adEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long adId) {

		try {
			AwarderDetailEntity adEntRef = manager.getReference(AwarderDetailEntity.class, adId);
	      	manager.remove(adEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AwarderDetail resource for id '%d' not found", adId));
		}
	}

}