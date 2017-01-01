package com.coupon.process.session.remote;

import javax.ejb.Remote;

import com.coupon.process.message.OfferNotificationReceiptMessage;
import com.coupon.process.message.OfferNotificationResponseMessage;
import com.coupon.process.message.OfferSetupReceiptMessage;


@Remote
public interface SourcingRemote {

	public static final String NAME = "Sourcing";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/Sourcing";
	
	public void setup(long couponId);

	public void setupAcknowledge(OfferSetupReceiptMessage osrm);

	public void notify(long couponId);

	public void notifyAcknowledge(OfferNotificationReceiptMessage onrm);

	public void notifyRespond(OfferNotificationResponseMessage onrm);

}