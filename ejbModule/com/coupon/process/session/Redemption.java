package com.coupon.process.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.GlobalTradeIdentificationNumber;
import com.coupon.common.RedemptionPeriod;
import com.coupon.common.PurchaseRequirement;
import com.coupon.common.PurchaseTradeItem;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.GlobalCouponNumberEntity;
import com.coupon.common.entity.GlobalLocationNumberEntity;
import com.coupon.common.entity.GlobalServiceRelationNumberEntity;
import com.coupon.common.entity.AwarderDetailEntity;
import com.coupon.common.entity.PurchaseTradeItemEntity;
import com.coupon.common.session.AwarderDetails;
import com.coupon.common.session.GlobalCouponNumbers;
import com.coupon.common.session.GlobalLocationNumbers;
import com.coupon.common.session.GlobalServiceRelationNumbers;
import com.coupon.common.session.GlobalTradeIdentificationNumbers;
import com.coupon.common.session.Offers;
import com.coupon.common.bean.GlobalCouponNumberBean;
import com.coupon.process.entity.CouponAcquisitionEntity;
import com.coupon.process.entity.CouponRedemptionRecordEntity;
import com.coupon.process.entity.CouponRewardEntity;
import com.coupon.process.entity.CouponRewardLoyaltyPointEntity;
import com.coupon.process.entity.CouponRewardTradeItemEntity;
import com.coupon.process.entity.QualifyingPurchaseEntity;
import com.coupon.process.entity.QualifyingPurchaseTradeItemEntity;
import com.coupon.process.entity.ValidatePurchaseEntity;
import com.coupon.process.entity.ValidatePurchaseTradeItemEntity;
import com.coupon.process.entity.ValidateRedemptionRecordEntity;
import com.coupon.process.message.CouponRedemptionRecordMessage;
import com.coupon.process.message.QualifyingTradeItemMsg;
import com.coupon.process.message.RedemptionNotificationMessage;
import com.coupon.process.message.RedemptionValidationRequestMessage;
import com.coupon.process.message.RedemptionValidationResponseMessage;
import com.coupon.process.message.RewardLoyaltyPointMsg;
import com.coupon.process.message.RewardTradeItemMsg;
import com.coupon.process.session.remote.RedemptionRemote;
import com.coupon.process.type.AcquisitionResponseType;
import com.coupon.process.type.ValidationResponseType;


@Remote
@Stateless(name=RedemptionRemote.NAME, mappedName=RedemptionRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Redemption implements RedemptionRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB GlobalCouponNumbers gcnEJB;
	@EJB GlobalLocationNumbers glnEJB;
	@EJB GlobalServiceRelationNumbers gsrnEJB;
	@EJB GlobalTradeIdentificationNumbers gtinEJB;
	@EJB Offers oEJB;
	@EJB AwarderDetails adEJB;
	
	static final Logger logger = Logger.getLogger(Redemption.class.getName());

	public Redemption() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RedemptionValidationResponseMessage validateRedemption(RedemptionValidationRequestMessage requestMessage) {

		ValidateRedemptionRecordEntity vrrEntity = new ValidateRedemptionRecordEntity();
		ValidatePurchaseEntity vpEntity = new ValidatePurchaseEntity();
		List<ValidationResponseType> validationResponses = new ArrayList<ValidationResponseType>();

		GlobalCouponNumber offerNumber = new GlobalCouponNumberBean(
												requestMessage.getCouponNumber().getCompanyPrefix(),
												requestMessage.getCouponNumber().getCouponReference(),
												requestMessage.getCouponNumber().getCheckDigit(),
												GlobalCouponNumber.OFFER_SERIAL_COMPONENT);

		OfferEntity oEntity = oEJB.findEntityByCouponNumber(offerNumber);

		// validationResponses.add(ValidationResponseType.DOES_NOT_EXIST);

		//TODO check if coupon setup is completed

		if (requestMessage.getValidateDateTime().before(oEntity.getTimePeriod().getStartDateTime())) {
			
			validationResponses.add(ValidationResponseType.INACTIVE);
		}
		
		if (requestMessage.getValidateDateTime().after(oEntity.getTimePeriod().getEndDateTime())) {
			
			validationResponses.add(ValidationResponseType.EXPIRED);
		}
		vrrEntity.setValidateDateTime(requestMessage.getValidateDateTime());
		
		AwarderDetailEntity adEntity = null;
		GlobalLocationNumberEntity awarderNumberEntity = glnEJB.findEntityByNumber(requestMessage.getAwarderNumber());
		try {

			adEntity = adEJB.findEntityByCouponAndAwarderNumber(oEntity.getOfferNumber(), awarderNumberEntity);
		}
		catch(NoResultException ex) {

			validationResponses.add(ValidationResponseType.AWARDER_NOT_ALLOWED);
		}
		vrrEntity.setAwarderNumber(awarderNumberEntity);
		
		//check if a redemption period is active for redemption date
		boolean redemptionPeriodActive = false;
		for (RedemptionPeriod rp : adEntity.getRedemptionPeriods()) {
			
			if (rp.getTimePeriod().encloses(requestMessage.getValidateDateTime())) {
				
				redemptionPeriodActive = true;
			}
		}
		if (redemptionPeriodActive == false) {

			validationResponses.add(ValidationResponseType.INVALID_REDEMPTION_PERIOD);
		}

		//check if coupon was acquired by customer
		CouponAcquisitionEntity caEntity = null;
		GlobalCouponNumberEntity couponInstanceEntity = gcnEJB.findEntityByNumber(requestMessage.getCouponNumber()); 
		GlobalServiceRelationNumberEntity accountNumberEntity = gsrnEJB.findEntityByNumber(requestMessage.getAccountNumber());
		try {

			TypedQuery<CouponAcquisitionEntity> query = null;
			if (requestMessage.getAccountNumber() != null) {

				query = manager.createNamedQuery("CouponAcquisitionEntity.findByCouponAndAccountNumber",
													CouponAcquisitionEntity.class);
				query.setParameter("couponInstance", couponInstanceEntity);
				query.setParameter("accountNumber", accountNumberEntity);
				caEntity = query.getSingleResult();
			}
			else if (requestMessage.getAlternateAccountId() != null) {
			
				query = manager.createNamedQuery("CouponAcquisitionEntity.findByCouponAndAlternateAccountId",
													CouponAcquisitionEntity.class);
				query.setParameter("couponInstance", couponInstanceEntity);
				query.setParameter("alternateAccountId", requestMessage.getAlternateAccountId());
				caEntity = query.getSingleResult();
			}
		}
		catch (NoResultException ex) {

			validationResponses.add(ValidationResponseType.CUSTOMER_NOT_ALLOWED);
		}
		vrrEntity.setCouponInstance(couponInstanceEntity);
		vrrEntity.setAccountNumber(accountNumberEntity);
		vrrEntity.setAlternateAccountId(requestMessage.getAlternateAccountId());
		
		if ((caEntity != null) &&
			(caEntity.getResponseCode() != AcquisitionResponseType.REDEEMABLE)) {
			
			validationResponses.add(ValidationResponseType.CUSTOMER_NOT_ALLOWED);
		}

		short numberOfUses = 0;
		PurchaseRequirement pr = oEntity.getPurchaseRequirement();
		Map<GlobalTradeIdentificationNumber, PurchaseTradeItemEntity> ptiMap = 
				oEntity.getPurchaseRequirement().getPurchaseTradeItemsMap();
		Map<GlobalTradeIdentificationNumber, ValidatePurchaseTradeItemEntity> purchaseTradeItemsMap =
				new HashMap<GlobalTradeIdentificationNumber, ValidatePurchaseTradeItemEntity>();

		switch (pr.getPurchaseRequirementType()) {
		
		case SPECIFIED_PURCHASE_AMOUNT:
			if (requestMessage.getPurchaseMonetaryAmount() < pr.getPurchaseMonetaryAmount()) {
				
				validationResponses.add(ValidationResponseType.PURCHASE_AMOUNT_BELOW_REQUIRED);
			}
			else {
				
				numberOfUses = 1;
			}
			break;
			
		case ALL_SPECIFIED_ITEMS:
			
			Map<GlobalTradeIdentificationNumber, QualifyingTradeItemMsg> itemValidateMap = 
						new HashMap<GlobalTradeIdentificationNumber, QualifyingTradeItemMsg>();
			for (PurchaseTradeItem pti : pr.getPurchaseTradeItems()) {
				
				itemValidateMap.put(pti.getTradeItemNumber(), null);
			}
			
			for (QualifyingTradeItemMsg qtim : requestMessage.getPurchaseTradeItems()) {
				
				if (ptiMap.containsKey(qtim.getTradeItemNumber())) {
					
					PurchaseTradeItem pti = ptiMap.get(qtim.getTradeItemNumber());
					if (qtim.getTradeItemNumber().equals(pti.getTradeItemNumber()) &&
							(qtim.getTradeItemQuantity() >= pti.getTradeItemQuantity())) {

						short numberOfQualifiers = (short) (qtim.getTradeItemQuantity() / pti.getTradeItemQuantity());
						if ((numberOfUses == 0) || (numberOfQualifiers < numberOfUses)) {
							
							numberOfUses = numberOfQualifiers;
						}

						itemValidateMap.putIfAbsent(pti.getTradeItemNumber(), qtim);
					}
				}
				//replace with common code
				ValidatePurchaseTradeItemEntity vptiEntity = new ValidatePurchaseTradeItemEntity();
				vptiEntity.setTradeItemNumber(gtinEJB.findEntityByNumber(qtim.getTradeItemNumber()));
				vptiEntity.setTradeItemQuantity(qtim.getTradeItemQuantity());
				vptiEntity.setTradeItemGroup(qtim.getTradeItemGroup());
				vptiEntity.setValidatePurchase(vpEntity);
				purchaseTradeItemsMap.put(qtim.getTradeItemNumber(), vptiEntity);
			}
			
			for (QualifyingTradeItemMsg qtim : itemValidateMap.values()) {
				
				if (qtim == null) {
					
					validationResponses.add(ValidationResponseType.QUALIFYING_TRADE_ITEM_MISSING);
					break;
				}
			}
			break;
		
		case ONE_ITEM_PER_GROUP:
			
			Map<String, QualifyingTradeItemMsg> groupValidateMap = new HashMap<String, QualifyingTradeItemMsg>();
			for (PurchaseTradeItem pti : pr.getPurchaseTradeItems()) {
				
				groupValidateMap.put(pti.getTradeItemGroup(), null);
			}
			
			for (QualifyingTradeItemMsg qtim : requestMessage.getPurchaseTradeItems()) {
				
				if (ptiMap.containsKey(qtim.getTradeItemNumber())) {
					
					PurchaseTradeItem pti = ptiMap.get(qtim.getTradeItemNumber());
					if (qtim.getTradeItemNumber().equals(pti.getTradeItemNumber()) &&
							qtim.getTradeItemGroup().equals(pti.getTradeItemGroup()) &&
							(qtim.getTradeItemQuantity() >= pti.getTradeItemQuantity())) {

						short numberOfQualifiers = (short) (qtim.getTradeItemQuantity() / pti.getTradeItemQuantity());
						if ((numberOfUses == 0) || (numberOfQualifiers < numberOfUses)) {
							
							numberOfUses = numberOfQualifiers;
						}

						groupValidateMap.putIfAbsent(pti.getTradeItemGroup(), qtim);
					}
				}
				//replace with common code
				ValidatePurchaseTradeItemEntity vptiEntity = new ValidatePurchaseTradeItemEntity();
				vptiEntity.setTradeItemNumber(gtinEJB.findEntityByNumber(qtim.getTradeItemNumber()));
				vptiEntity.setTradeItemQuantity(qtim.getTradeItemQuantity());
				vptiEntity.setTradeItemGroup(qtim.getTradeItemGroup());
				vptiEntity.setValidatePurchase(vpEntity);
				purchaseTradeItemsMap.put(qtim.getTradeItemNumber(), vptiEntity);
			}
			
			for (QualifyingTradeItemMsg qtim : groupValidateMap.values()) {
				
				if (qtim == null) {
					
					validationResponses.add(ValidationResponseType.TRADE_ITEM_PER_GROUP_NOT_QUALIFIED);
					break;
				}
			}
			break;
		
		case ONE_OF_SPECIFIED_ITEMS:
			
			boolean oneOfSpecifiedItems = false;
			for (QualifyingTradeItemMsg qtim : requestMessage.getPurchaseTradeItems()) {
				
				if (ptiMap.containsKey(qtim.getTradeItemNumber())) {
					
					PurchaseTradeItem pti = ptiMap.get(qtim.getTradeItemNumber());
					if (qtim.getTradeItemNumber().equals(pti.getTradeItemNumber()) &&
							(qtim.getTradeItemQuantity() >= pti.getTradeItemQuantity())) {


						short numberOfQualifiers = (short) (qtim.getTradeItemQuantity() / pti.getTradeItemQuantity());
						if ((numberOfUses == 0) || (numberOfQualifiers > numberOfUses)) {
							
							numberOfUses = numberOfQualifiers;
						}

						oneOfSpecifiedItems = true;
					}
				}
				//replace with common code
				ValidatePurchaseTradeItemEntity vptiEntity = new ValidatePurchaseTradeItemEntity();
				vptiEntity.setTradeItemNumber(gtinEJB.findEntityByNumber(qtim.getTradeItemNumber()));
				vptiEntity.setTradeItemQuantity(qtim.getTradeItemQuantity());
				vptiEntity.setTradeItemGroup(qtim.getTradeItemGroup());
				vptiEntity.setValidatePurchase(vpEntity);
				purchaseTradeItemsMap.put(qtim.getTradeItemNumber(), vptiEntity);
			}
			if (oneOfSpecifiedItems == false) {
				
				validationResponses.add(ValidationResponseType.QUALIFYING_TRADE_ITEM_MISSING);
			}
			break;
		}
		vpEntity.setPurchaseTradeItemsMap(purchaseTradeItemsMap);
		
		if (numberOfUses > oEntity.getUsageCondition().getMaximumUsePerTransaction()) {

			numberOfUses = oEntity.getUsageCondition().getMaximumUsePerTransaction();
		}

		List<CouponRedemptionRecordEntity> redemptionRecords = new ArrayList<CouponRedemptionRecordEntity>();
		TypedQuery<CouponRedemptionRecordEntity> query = null;
		if (requestMessage.getAccountNumber() != null) {

			query = manager.createNamedQuery("CouponRedemptionRecordEntity.findByCouponAndAccountNumber",
												CouponRedemptionRecordEntity.class);
			query.setParameter("couponInstance", couponInstanceEntity);
			query.setParameter("accountNumber", accountNumberEntity);
			redemptionRecords = query.getResultList();
		}
		else if (requestMessage.getAlternateAccountId() != null) {
		
			query = manager.createNamedQuery("CouponRedemptionRecordEntity.findByCouponAndAlternateAccountId",
												CouponRedemptionRecordEntity.class);
			query.setParameter("couponInstance", couponInstanceEntity);
			query.setParameter("alternateAccountId", requestMessage.getAlternateAccountId());
			redemptionRecords = query.getResultList();
		}
		if (redemptionRecords.size() >= oEntity.getUsageCondition().getMaximumCumulativeUse()) {

			validationResponses.add(ValidationResponseType.MAX_CUSTOMER_REDEMPTIONS_REACHED);
		}

//TODO		validationResponses.add(ValidationResponseType.MAX_TOTAL_REDEMPTIONS_REACHED);

		RedemptionValidationResponseMessage responseMessage = new RedemptionValidationResponseMessage();
		if (validationResponses.isEmpty()) {
			
			validationResponses.add(ValidationResponseType.VALID);
			responseMessage.setRedeemable(true);
		}
		else {
			
			responseMessage.setRedeemable(false);
		}
		vrrEntity.setRedeemable(responseMessage.isRedeemable());
		responseMessage.setResponses(validationResponses);

		vrrEntity.setValidatePurchase(vpEntity);
		vpEntity.setValidateRedemptionRecord(vrrEntity);
		manager.persist(vrrEntity);
		responseMessage.setValidationReference(vrrEntity.getId());
	
		return responseMessage;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void notifyRedemption(RedemptionNotificationMessage rnm) {

		for (CouponRedemptionRecordMessage crrMessage : rnm.getRedemptionRecords()) {
			
			CouponRedemptionRecordEntity crrEntity = new CouponRedemptionRecordEntity();
			crrEntity.setStoreGln(glnEJB.findEntityByNumber(crrMessage.getStoreGln()));
			crrEntity.setStoreInternalId(crrMessage.getStoreInternalId());
			crrEntity.setPosTerminalId(crrMessage.getPosTerminalId());
			crrEntity.setCouponInstance(gcnEJB.findEntityByNumber(crrMessage.getCouponInstance()));
			crrEntity.setAccountNumber(gsrnEJB.findEntityByNumber(crrMessage.getAccountNumber()));
			crrEntity.setAlternateAccountId(crrMessage.getAlternateAccountId());
			crrEntity.setRedemptionDateTime(crrMessage.getRedemptionDateTime());
			
			CouponRewardEntity crEntity = new CouponRewardEntity();
			crEntity.setRewardMonetaryAmount(crrMessage.getRewardMonetaryAmount());
			if (!crrMessage.getRewardLoyaltyPoints().isEmpty()) {

				Map<String, CouponRewardLoyaltyPointEntity> loyaltyPointsMap = 
						new HashMap<String, CouponRewardLoyaltyPointEntity>();
				for (RewardLoyaltyPointMsg rlpm : crrMessage.getRewardLoyaltyPoints()) {

					CouponRewardLoyaltyPointEntity crlpEntity = new CouponRewardLoyaltyPointEntity();
					crlpEntity.setLoyaltyProgramName(rlpm.getLoyaltyProgramName());
					crlpEntity.setLoyaltyPointsQuantity(rlpm.getLoyaltyPointsQuantity());
					crlpEntity.setReward(crEntity);
					
					loyaltyPointsMap.put(crlpEntity.getLoyaltyProgramName(), crlpEntity);
				}
				crEntity.setRewardedLoyaltyPointsMap(loyaltyPointsMap);
			}
			if (!crrMessage.getRewardTradeItems().isEmpty()) {
				
				Map<GlobalTradeIdentificationNumber, CouponRewardTradeItemEntity> tradeItemsMap = 
						new HashMap<GlobalTradeIdentificationNumber, CouponRewardTradeItemEntity>();
				for (RewardTradeItemMsg rtim : crrMessage.getRewardTradeItems()) {

					CouponRewardTradeItemEntity crtiEntity = new CouponRewardTradeItemEntity();
					crtiEntity.setTradeItemNumber(gtinEJB.findEntityByNumber(rtim.getTradeItemNumber()));
					crtiEntity.setTradeItemQuantity(rtim.getTradeItemQuantity());
					crtiEntity.setReward(crEntity);
					
					tradeItemsMap.put(crtiEntity.getTradeItemNumber(), crtiEntity);
				}
				crEntity.setRewardedTradeItemsMap(tradeItemsMap);
			}
			crEntity.setCouponRedemptionRecord(crrEntity);
			crrEntity.setCouponReward(crEntity);
		
			QualifyingPurchaseEntity qpEntity = new QualifyingPurchaseEntity();
			qpEntity.setPurchaseMonetaryAmount(crrMessage.getQualifyingPurchaseAmount());
			
			Map<GlobalTradeIdentificationNumber, QualifyingPurchaseTradeItemEntity> tradeItemsMap = 
					new HashMap<GlobalTradeIdentificationNumber, QualifyingPurchaseTradeItemEntity>();
			for (QualifyingTradeItemMsg qtim : crrMessage.getQualifyingTradeItems()) {

				QualifyingPurchaseTradeItemEntity qptiEntity = new QualifyingPurchaseTradeItemEntity();
				qptiEntity.setTradeItemNumber(gtinEJB.findEntityByNumber(qtim.getTradeItemNumber()));
				qptiEntity.setTradeItemQuantity(qtim.getTradeItemQuantity());
				qptiEntity.setTradeItemGroup(qtim.getTradeItemGroup());
				qptiEntity.setQualifyingPurchase(qpEntity);
				
				tradeItemsMap.put(qptiEntity.getTradeItemNumber(), qptiEntity);
			}
			qpEntity.setPurchaseTradeItemsMap(tradeItemsMap);
			qpEntity.setCouponRedemptionRecord(crrEntity);
			crrEntity.setQualifyingPurchase(qpEntity);
			
			crrEntity.setValidationOverrideReference(crrMessage.getValidationOverrideReference());
			
			manager.persist(crrEntity);
		}
	}

}