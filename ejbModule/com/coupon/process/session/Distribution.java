package com.coupon.process.session;

import java.util.Date;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidationException;

import com.coupon.common.AcquisitionPeriod;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.GlobalCouponNumberEntity;
import com.coupon.common.session.GlobalCouponNumbers;
import com.coupon.common.session.GlobalServiceRelationNumbers;
import com.coupon.common.session.Offers;
import com.coupon.process.entity.CouponAcquisitionEntity;
import com.coupon.process.message.AcquireCouponMessage;
import com.coupon.process.message.AcquisitionConfirmationMessage;
import com.coupon.process.message.AcquisitionNotificationMessage;
import com.coupon.process.message.RedeemableAcknowledgementMessage;
import com.coupon.process.session.remote.DistributionRemote;
import com.coupon.process.type.AcquisitionResponseType;


@Remote
@Stateless(name=DistributionRemote.NAME, mappedName=DistributionRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Distribution implements DistributionRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB Offers oEJB;
	@EJB GlobalCouponNumbers gcnEJB;
	@EJB GlobalServiceRelationNumbers gsrnEJB;

	static final Logger logger = Logger.getLogger(Distribution.class.getName());

	public Distribution() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AcquisitionConfirmationMessage acquire(AcquireCouponMessage couponMessage) {

		OfferEntity oEntity = oEJB.findEntityByCouponNumber(couponMessage.getCouponNumber());

		//TODO check if coupon setup is completed

		//check if an acquisition period is active for acquisition date
		boolean acquisitionPeriodActive = false;
		for (AcquisitionPeriod ap : oEntity.getDistributionDetail().getAcquisitionPeriods()) {
			
			if (ap.getTimePeriod().encloses(couponMessage.getAcquisitionDateTime())) {
				
				acquisitionPeriodActive = true;
			}
		}
		if (acquisitionPeriodActive == false) {

			throw new ValidationException("acquisitionDateTime does not fall within allowed acquisition periods.");
		}

		//check if there is room for more acquisitions
		if (oEntity.getDistributionDetail().getTotalAcquisitionCount() >=
				oEntity.getDistributionDetail().getMaximumOfferAcquisition()) {
			
			throw new ValidationException("No more coupons can be acquired for offer (maximum allowable coupons already acquired).");
		}
		
		oEntity.getDistributionDetail().setTotalAcquisitionCount(
				oEntity.getDistributionDetail().getTotalAcquisitionCount() + 1);

		CouponAcquisitionEntity caEntity = new CouponAcquisitionEntity();
		caEntity.setAccountNumber(gsrnEJB.findEntityByNumber(couponMessage.getAccountNumber()));
		caEntity.setAlternateAccountId(couponMessage.getAlternateAccountId());
		caEntity.setAcquisitionDateTime(new Date());

		GlobalCouponNumberEntity couponInstanceEntity = gcnEJB.createEntity(couponMessage.getCouponNumber());
		couponInstanceEntity.setSerialComponent(oEntity.getDistributionDetail().getTotalAcquisitionCount());
		caEntity.setCouponInstance(couponInstanceEntity);
		
		//build acquisition message to notify awarder
		AcquisitionNotificationMessage notifyMessage = new AcquisitionNotificationMessage();
		notifyMessage.setCouponInstance(couponInstanceEntity);
		notifyMessage.setAccountNumber(couponMessage.getAccountNumber());
		notifyMessage.setAlternateAccountId(couponMessage.getAlternateAccountId());

		//notify awarder
		RedeemableAcknowledgementMessage acknowledgeMessage = notifyAcquisition(notifyMessage);

		caEntity.setResponseCode(acknowledgeMessage.getResponseCode());
		caEntity.setAcknowledgementDateTime(acknowledgeMessage.getAcknowledgeDateTime());

		manager.persist(caEntity);
		manager.persist(oEntity);
		
		AcquisitionConfirmationMessage confirmMessage = new AcquisitionConfirmationMessage();
		confirmMessage.setCouponInstance(couponInstanceEntity);
		confirmMessage.setResponseCode(AcquisitionResponseType.REDEEMABLE);
		
		return confirmMessage;
	}

	public RedeemableAcknowledgementMessage notifyAcquisition(AcquisitionNotificationMessage notifyMessage) {

		//TODO check if awarder needs to be notified

		RedeemableAcknowledgementMessage acknowledgeMessage = new RedeemableAcknowledgementMessage();
		acknowledgeMessage.setCouponInstance(notifyMessage.getCouponInstance());
		acknowledgeMessage.setResponseCode(AcquisitionResponseType.REDEEMABLE);
		acknowledgeMessage.setAcknowledgeDateTime(new Date());
		
		return acknowledgeMessage;
	}

}