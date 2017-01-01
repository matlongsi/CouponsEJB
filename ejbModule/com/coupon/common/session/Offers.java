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
import javax.persistence.TypedQuery;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.GlobalLocationNumber;
import com.coupon.common.TimePeriod;
import com.coupon.common.bean.AcquisitionPeriodBean;
import com.coupon.common.bean.AwarderDetailBean;
import com.coupon.common.bean.OfferBean;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.AwarderDetailEntity;
import com.coupon.common.entity.DistributionDetailEntity;
import com.coupon.common.entity.FinancialSettlementDetailEntity;
import com.coupon.common.entity.MarketingMaterialEntity;
import com.coupon.common.entity.RewardEntity;
import com.coupon.common.entity.UsageConditionEntity;
import com.coupon.common.entity.PurchaseRequirementEntity;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.OffersRemote;
import com.coupon.common.utils.TimePeriodHelper;


@Remote
@LocalBean
@Stateless(name=OffersRemote.NAME, mappedName=OffersRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Offers implements OffersRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private GlobalLocationNumbers glnEJB;
	@EJB private GlobalCouponNumbers gcnEJB;
	@EJB private MarketingMaterials mmEJB;
	@EJB private DistributionDetails ddEJB;
	@EJB private AwarderDetails adEJB;
	@EJB private Rewards rEJB;
	@EJB private PurchaseRequirements prEJB;
	@EJB private UsageConditions ucEJB;
	@EJB private FinancialSettlementDetails fsdEJB;
	
	static final Logger logger = Logger.getLogger(Offers.class.getName());

	public Offers() {
	}

	public void validate(OfferEntity oe) throws DataValidationException {

		if (oe.getDistributionDetail() != null) {
			ddEJB.validate(oe.getDistributionDetail());
		}
	    	if (oe.getMarketingMaterial() != null) {
	    		mmEJB.validate(oe.getMarketingMaterial());
	    	}
	    	if (oe.getReward() != null) {
	    		rEJB.validate(oe.getReward());
	    	}
	    	if (oe.getPurchaseRequirement() != null) {
	    		prEJB.validate(oe.getPurchaseRequirement());
	    	}

	    	for (AwarderDetailEntity ade : oe.getAwarderDetails()) {
			adEJB.validate(ade);
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OfferEntity findEntity(long oId) {

		OfferEntity oEnt = manager.find(OfferEntity.class, oId);		
	  	if (oEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("Offer resource with id '%d' not found", oId));
	  	}

		return oEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public OfferBean find(long oId) {

		OfferBean o = new OfferBean().init(findEntity(oId));
		TimePeriod tp = TimePeriodHelper.toTimeZone(
							o.getTimePeriod(),
							TimeZone.getTimeZone(o.getTimeZone()));
		o.setTimePeriod(tp);

		return o;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OfferEntity findEntityByCouponNumber(GlobalCouponNumber gcn) {

		TypedQuery<OfferEntity> query = manager.createNamedQuery("OfferEntity.findByCouponNumber", OfferEntity.class);

		return query.setParameter("offerNumber", gcnEJB.findEntityByNumber(gcn)).getSingleResult();
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public OfferBean findByCouponNumber(GlobalCouponNumber gcn) {

		OfferBean o = new OfferBean().init(findEntityByCouponNumber(gcn));
		TimePeriod tp = TimePeriodHelper.toTimeZone(
							o.getTimePeriod(),
							TimeZone.getTimeZone(o.getTimeZone()));
		o.setTimePeriod(tp);

		return o;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public OfferEntity createEntity(OfferBean o) {

		OfferEntity oEnt = new OfferEntity();
		oEnt.setIssuerNumber(glnEJB.findEntityByNumber(o.getIssuerNumber()));
		oEnt.setDistributorNumber(glnEJB.findEntityByNumber(o.getDistributorNumber()));
		oEnt.setOfferNumber(gcnEJB.createEntity(o.getOfferNumber()));
		oEnt.setOfferType(o.getOfferType());
		oEnt.setOfferStatus(o.getOfferStatus());
		oEnt.setTimeZone(o.getTimeZone());
		oEnt.setTimePeriod(o.getTimePeriod());

	    	if (o.getIssuerClearingAgentNumber() != null) {
	    		oEnt.setIssuerClearingAgentNumber(
	    				glnEJB.findEntityByNumber(o.getIssuerClearingAgentNumber()));
	    	}
	    	if (o.getDistributionDetail() != null) {
	 
	    		DistributionDetailEntity ddEntity = ddEJB.createEntity(o.getDistributionDetail()); 
	    		oEnt.setDistributionDetail(ddEntity);
	    		ddEntity.setOffer(oEnt);
	    	}
	    	if (o.getMarketingMaterial() != null) {
	    		
	    		MarketingMaterialEntity mmEntity = mmEJB.createEntity(o.getMarketingMaterial()); 
	    		oEnt.setMarketingMaterial(mmEntity);
	    		mmEntity.setOffer(oEnt);
	    	}
	    	if (o.getReward() != null) {
	 
	    		RewardEntity rEntity = rEJB.createEntity(o.getReward()); 
	    		oEnt.setReward(rEntity);
	    		rEntity.setOffer(oEnt);
	    	}
	    	if (o.getPurchaseRequirement() != null) {
	
	    		PurchaseRequirementEntity prEntity = prEJB.createEntity(o.getPurchaseRequirement());
	    		oEnt.setPurchaseRequirement(prEntity);
	    		prEntity.setOffer(oEnt);
	    	}
	    	if (o.getUsageCondition() != null) {
	
	    		UsageConditionEntity ucEntity = ucEJB.createEntity(o.getUsageCondition()); 
	    		oEnt.setUsageCondition(ucEntity);
	    		ucEntity.setOffer(oEnt);
	    	}
	    	if (o.getFinancialSettlementDetail() != null) {
	    		
	    		FinancialSettlementDetailEntity fsdEntity = fsdEJB.createEntity(o.getFinancialSettlementDetail());
	    		oEnt.setFinancialSettlementDetail(fsdEntity);
	    		fsdEntity.setOffer(oEnt);
	    	}
	
	    	if (o.getAwarderDetails() != null) {

			Map<GlobalLocationNumber, AwarderDetailEntity> adMap = new HashMap<GlobalLocationNumber, AwarderDetailEntity>();
			for (AwarderDetailBean ad : o.getAwarderDetails()) {
	
				AwarderDetailEntity adEntity = adEJB.createEntity(ad);
				adEntity.setOffer(oEnt);
				adMap.put(adEntity.getAwarderNumber(), adEntity);
			}
			oEnt.setAwarderDetailsMap(adMap);
	    	}
	
		return oEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public OfferBean create(OfferBean o) {

		TimePeriod tp = TimePeriodHelper.toDefault(
				o.getTimePeriod(),
				TimeZone.getTimeZone(o.getTimeZone()));
		o.setTimePeriod(tp);
		if (o.getDistributionDetail() != null) {

			tp = TimePeriodHelper.toDefault(
					o.getDistributionDetail().getPublicationPeriod().getTimePeriod(),
					TimeZone.getTimeZone(o.getTimeZone()));
			o.getDistributionDetail().getPublicationPeriod().setTimePeriod(tp);
			for (AcquisitionPeriodBean ap : o.getDistributionDetail().getAcquisitionPeriods()) {
	
				tp = TimePeriodHelper.toDefault(
						ap.getTimePeriod(),
						TimeZone.getTimeZone(o.getTimeZone()));
				ap.setTimePeriod(tp);
			}
		}
		OfferEntity oEnt = createEntity(o);
		validate(oEnt);

		manager.persist(oEnt);
		manager.flush();

		OfferBean oAdd = new OfferBean().init(oEnt);
		tp = TimePeriodHelper.toTimeZone(
				oAdd.getTimePeriod(),
				TimeZone.getTimeZone(oAdd.getTimeZone()));
		oAdd.setTimePeriod(tp);

		return oAdd;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public OfferEntity updateEntity(OfferBean o) {

		OfferEntity oEntRef;
		try {

			oEntRef = manager.getReference(OfferEntity.class, o.getId());
		    	if (o.getIssuerClearingAgentNumber() != null) {
		    		oEntRef.setIssuerClearingAgentNumber(glnEJB.findEntityByNumber(o.getIssuerClearingAgentNumber()));
		    	}
			oEntRef.setIssuerNumber(glnEJB.findEntityByNumber(o.getIssuerNumber()));
			oEntRef.setDistributorNumber(glnEJB.findEntityByNumber(o.getDistributorNumber()));
			oEntRef.setOfferNumber(gcnEJB.updateEntity(o.getOfferNumber()));
			oEntRef.setOfferType(o.getOfferType());
			oEntRef.setTimeZone(o.getTimeZone());
			oEntRef.setOfferStatus(o.getOfferStatus());
	
			TimePeriod tp = TimePeriodHelper.toDefault(
								o.getTimePeriod(),
								TimeZone.getTimeZone(o.getTimeZone()));
			oEntRef.setTimePeriod(tp);
	
		    	if (o.getDistributionDetail() != null) {
		    		oEntRef.setDistributionDetail(ddEJB.updateEntity(o.getDistributionDetail()));
		    	}
		    	if (o.getMarketingMaterial() != null) {
		    		oEntRef.setMarketingMaterial(mmEJB.updateEntity(o.getMarketingMaterial()));
		    	}
		    	if (o.getReward() != null) {
		    		oEntRef.setReward(rEJB.updateEntity(o.getReward()));
		    	}
		    	if (o.getPurchaseRequirement() != null) {
		    		oEntRef.setPurchaseRequirement(prEJB.updateEntity(o.getPurchaseRequirement()));
		    	}
		    	if (o.getUsageCondition() != null) {
		    		oEntRef.setUsageCondition(ucEJB.updateEntity(o.getUsageCondition()));
		    	}
		    	if (o.getFinancialSettlementDetail() != null) {
		    		oEntRef.setFinancialSettlementDetail(fsdEJB.updateEntity(o.getFinancialSettlementDetail()));
		    	}
	
			for (AwarderDetailBean ad : o.getAwarderDetails()) {
				oEntRef.getAwarderDetailsMap().put(ad.getAwarderNumber(), adEJB.updateEntity(ad));
			}
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Offer resource for id '%d' not found", o.getId()));
		}

		return oEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public OfferBean update(OfferBean o) {

		OfferEntity oEntRef = updateEntity(o);
		validate(oEntRef);

		OfferBean oUpdt = new OfferBean().init(oEntRef);
		TimePeriod tp = TimePeriodHelper.toTimeZone(
				oUpdt.getTimePeriod(),
				TimeZone.getTimeZone(oUpdt.getTimeZone()));
		oUpdt.setTimePeriod(tp);

		return oUpdt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long oId) {

		try {
			OfferEntity oEntRef = manager.getReference(OfferEntity.class, oId);
	      	manager.remove(oEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Offer resource for id '%d' not found", oId));
		}
	}

}