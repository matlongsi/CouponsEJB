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
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.TimePeriod;
import com.coupon.common.entity.GlobalCouponNumberEntity;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.AcquisitionPeriodEntity;
import com.coupon.common.entity.DistributionDetailEntity;
import com.coupon.common.entity.PublicationPeriodEntity;
import com.coupon.common.entity.TimePeriodEmbed;
import com.coupon.common.bean.AcquisitionPeriodBean;
import com.coupon.common.bean.DistributionDetailBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.DistributionDetailsRemote;
import com.coupon.common.utils.ExceptionHelper;
import com.coupon.common.utils.TimePeriodHelper;


@Remote
@LocalBean
@Stateless(name=DistributionDetailsRemote.NAME, mappedName=DistributionDetailsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class DistributionDetails implements DistributionDetailsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private AcquisitionPeriods apEJB;
	@EJB private Offers oEJB;
	@EJB private GlobalCouponNumbers gcnEJB;
	
	static final Logger logger = Logger.getLogger(DistributionDetails.class.getName());

	public DistributionDetails() {
	}

	public void validate(DistributionDetailEntity dde) throws DataValidationException {

		if (!dde.getOffer().getTimePeriod().encloses(dde.getPublicationPeriod().getTimePeriod())) {
			throw new DataValidationException(
					String.format("PublicationPeriod '%s' is not within Offer time period '%s'",
							dde.getPublicationPeriod().getTimePeriod().toString(),
							dde.getOffer().getTimePeriod().toString()));
		}

		for (AcquisitionPeriodEntity ape : dde.getAcquisitionPeriods()) {
			apEJB.validate(ape);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DistributionDetailEntity findEntity(long ddId) {

		DistributionDetailEntity ddEnt = manager.find(DistributionDetailEntity.class, ddId);		
	  	if (ddEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("DistributionDetail resource with id '%d' not found", ddId));
	  	}

		return ddEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public DistributionDetailBean find(long ddId) {

		DistributionDetailEntity ddEnt = findEntity(ddId);

		DistributionDetailBean ddb = new DistributionDetailBean().init(ddEnt);
		TimePeriod tp = TimePeriodHelper.toTimeZone(
				ddb.getPublicationPeriod().getTimePeriod(),
				TimeZone.getTimeZone(ddEnt.getOffer().getTimeZone()));
		ddb.getPublicationPeriod().setTimePeriod(tp);
		for (AcquisitionPeriodBean ap : ddb.getAcquisitionPeriods()) {

			tp = TimePeriodHelper.toTimeZone(
					ap.getTimePeriod(),
					TimeZone.getTimeZone(ddEnt.getOffer().getTimeZone()));
			ap.setTimePeriod(tp);
		}

		return ddb;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public DistributionDetailEntity findEntityByCouponNumber(GlobalCouponNumberEntity couponNumberEntity) {

		TypedQuery<DistributionDetailEntity> query = manager
				.createNamedQuery("DistributionDetailEntity.findByCouponNumber", DistributionDetailEntity.class)
				.setParameter("couponNumber", couponNumberEntity);
		
		return query.getSingleResult();
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public DistributionDetailBean findByCouponNumber(GlobalCouponNumber couponNumber) {

		GlobalCouponNumberEntity couponNumberEntity = gcnEJB.findEntityByNumber(couponNumber);
		
		return new DistributionDetailBean().init(findEntityByCouponNumber(couponNumberEntity));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public DistributionDetailEntity createEntity(DistributionDetailBean dd) {
		
		DistributionDetailEntity ddEnt = new DistributionDetailEntity();

		ddEnt.setMaximumOfferAcquisition(dd.getMaximumOfferAcquisition());
		ddEnt.setTotalAcquisitionCount(dd.getTotalAcquisitionCount());

		PublicationPeriodEntity ppEntity = new PublicationPeriodEntity();
		ppEntity.setTimePeriod(new TimePeriodEmbed().init(
				dd.getPublicationPeriod().getTimePeriod()));
		ppEntity.setDistributionDetail(ddEnt);
		ddEnt.setPublicationPeriod(ppEntity);

		Map<TimePeriod, AcquisitionPeriodEntity> apMap = new HashMap<TimePeriod, AcquisitionPeriodEntity>();
		for (AcquisitionPeriodBean ap : dd.getAcquisitionPeriods()) {

			AcquisitionPeriodEntity apEntity = apEJB.createEntity(ap);
			apEntity.setDistributionDetail(ddEnt);
			apMap.put(apEntity.getTimePeriod(), apEntity);
		}
		ddEnt.setAcquisitionPeriodsMap(apMap);

		return ddEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public DistributionDetailBean create(DistributionDetailBean dd) {

		OfferEntity oEnt = oEJB.findEntity(dd.getParentId());
		TimePeriod tp = TimePeriodHelper.toDefault(
				dd.getPublicationPeriod().getTimePeriod(),
				TimeZone.getTimeZone(oEnt.getTimeZone()));
		dd.getPublicationPeriod().setTimePeriod(tp);
		for (AcquisitionPeriodBean ap : dd.getAcquisitionPeriods()) {

			tp = TimePeriodHelper.toDefault(
					ap.getTimePeriod(),
					TimeZone.getTimeZone(oEnt.getTimeZone()));
			ap.setTimePeriod(tp);
		}

		DistributionDetailEntity ddEnt = createEntity(dd);
		ddEnt.setOffer(oEnt);
		validate(ddEnt);

      	try {
      		manager.persist(ddEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_DISTRIBUTION_DETAIL")) {
	    			throw new ResourceConflictException(
	    	  				String.format("DistributionDetail for Offer number '%s' already exists",
	    	  						oEnt.getOfferNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new DistributionDetailBean().init(ddEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public DistributionDetailEntity updateEntity(DistributionDetailBean dd) {

		DistributionDetailEntity ddEntRef;
		try {

			ddEntRef = manager.getReference(DistributionDetailEntity.class, dd.getId());
			ddEntRef.setMaximumOfferAcquisition(dd.getMaximumOfferAcquisition());
			ddEntRef.setTotalAcquisitionCount(dd.getTotalAcquisitionCount());
			ddEntRef.setPublicationPeriod(dd.getPublicationPeriod());
			manager.merge(ddEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("DistributionDetail resource for id '%d' not found", dd.getId()));
		}

		return ddEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public DistributionDetailBean update(DistributionDetailBean dd) {

		DistributionDetailEntity ddEntRef = updateEntity(dd);
		validate(ddEntRef);

		return new DistributionDetailBean().init(ddEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long ddId) {

		try {
			DistributionDetailEntity ddEntRef = manager.getReference(DistributionDetailEntity.class, ddId);
	      	manager.remove(ddEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("DistributionDetail resource for id '%d' not found", ddId));
		}
	}

}