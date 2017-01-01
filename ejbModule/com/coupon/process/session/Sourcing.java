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
import javax.persistence.TypedQuery;

import com.coupon.common.AwarderDetail;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.GlobalCouponNumberEntity;
import com.coupon.common.entity.GlobalLocationNumberEntity;
import com.coupon.common.entity.AwarderDetailEntity;
import com.coupon.common.session.AwarderDetails;
import com.coupon.common.session.GlobalCouponNumbers;
import com.coupon.common.session.GlobalLocationNumbers;
import com.coupon.common.session.Offers;
import com.coupon.process.entity.HeaderEmbed;
import com.coupon.process.entity.OfferNotificationEntity;
import com.coupon.process.entity.OfferSetupEntity;
import com.coupon.process.message.HeaderMsg;
import com.coupon.process.message.OfferNotificationMessage;
import com.coupon.process.message.OfferNotificationReceiptMessage;
import com.coupon.process.message.OfferNotificationResponseMessage;
import com.coupon.process.message.OfferSetupMessage;
import com.coupon.process.message.OfferSetupReceiptMessage;
import com.coupon.process.session.remote.SourcingRemote;


@Remote
@Stateless(name=SourcingRemote.NAME, mappedName=SourcingRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Sourcing implements SourcingRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB Offers oEJB;
	@EJB GlobalCouponNumbers gcnEJB;
	@EJB GlobalLocationNumbers glnEJB;
	@EJB AwarderDetails adEJB;

	static final Logger logger = Logger.getLogger(Sourcing.class.getName());

	public Sourcing() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void setup(long couponId) {

		OfferSetupEntity setupEntity = new OfferSetupEntity();
		HeaderEmbed headerEntity = new HeaderEmbed();
		OfferEntity oEntity = oEJB.findEntity(couponId);
		
		setupEntity.setCoupon(oEntity);
		headerEntity.setSender(oEntity.getIssuerNumber());
		headerEntity.setRecipient(oEntity.getDistributorNumber());
		headerEntity.setInitDateTime(new Date());
		setupEntity.setHeader(headerEntity);

		manager.persist(setupEntity);

		OfferSetupMessage osm = new OfferSetupMessage();
		HeaderMsg hm = new HeaderMsg();

		hm.setReferenceId(setupEntity.getId());
		hm.setSenderNumber(oEntity.getIssuerNumber());
		hm.setRecipientNumber(oEntity.getDistributorNumber());
		hm.setCreationDateTime(new Date());

		osm.setHeader(hm);
		osm.setOfferNumber(oEntity.getOfferNumber());
		osm.setDistributionDetail(oEntity.getDistributionDetail());
		osm.setDistributorNumber(oEntity.getDistributorNumber());
		osm.setFinancialSettlementDetail(oEntity.getFinancialSettlementDetail());
		osm.setIssuerNumber(oEntity.getIssuerNumber());
		osm.setAwarderDetails(oEntity.getAwarderDetails());
		osm.setReward(oEntity.getReward());
		osm.setOfferType(oEntity.getOfferType());
		osm.setPurchaseRequirement(oEntity.getPurchaseRequirement());
		osm.setTimePeriod(oEntity.getTimePeriod());
		osm.setTimeZone(oEntity.getTimeZone());
		osm.setUsageCondition(oEntity.getUsageCondition());
		
		//TODO send message to distributor
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void setupAcknowledge(OfferSetupReceiptMessage osrm) {

		OfferSetupEntity setupEntityRef = manager.getReference(OfferSetupEntity.class, osrm.getReference());
		
		if (setupEntityRef.getAcknowledgementDateTime() != null) {
			
			//TODO throw exception?
		}
		setupEntityRef.setAcknowledgementDateTime(osrm.getAcknowledgeDateTime());
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void notify(long couponId) {

		OfferEntity oEntity = oEJB.findEntity(couponId);
		
		for (AwarderDetail ad : oEntity.getAwarderDetails()) {

			HeaderEmbed headerEntity = new HeaderEmbed();
			headerEntity.setSender(oEntity.getIssuerNumber());
			headerEntity.setRecipient(ad.getAwarderNumber());
			headerEntity.setInitDateTime(new Date());
			OfferNotificationEntity notificationEntity = new OfferNotificationEntity();
			notificationEntity.setOffer(oEntity);
			notificationEntity.setHeader(headerEntity);

			manager.persist(notificationEntity);

			OfferNotificationMessage onm = new OfferNotificationMessage();
			HeaderMsg hm = new HeaderMsg();

			hm.setReferenceId(notificationEntity.getId());
			hm.setSenderNumber(oEntity.getIssuerNumber());
			hm.setRecipientNumber(ad.getAwarderNumber());
			hm.setCreationDateTime(new Date());
			
			onm.setHeader(hm);
			onm.setIssuerNumber(oEntity.getIssuerNumber());
			if (oEntity.getIssuerClearingAgentNumber() != null) {

				onm.setIssuerClearingAgentNumber(oEntity.getIssuerClearingAgentNumber());
			}
			onm.setAwarderNumber(ad.getAwarderNumber());
			
			/**
			 * Date the coupon offer should be accepted by the Offer Awarder
			 */
			onm.setLatestAcceptanceDate(oEntity.getTimePeriod().getStartDateTime());
			
			onm.setCouponNumber(oEntity.getOfferNumber());
			onm.setTimePeriod(oEntity.getTimePeriod());
			onm.setOfferTypeCode(oEntity.getOfferType());
			onm.setUsageCondition(oEntity.getUsageCondition());
			onm.setFinancialSettlementDetail(oEntity.getFinancialSettlementDetail());
			onm.setAwarderPointOfSales(ad.getPointOfSales());
			onm.setOfferReward(oEntity.getReward());
			onm.setPurchaseRequirement(oEntity.getPurchaseRequirement());

			//TODO send message to awarder

		}

	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void notifyAcknowledge(OfferNotificationReceiptMessage onrm) {

		OfferNotificationEntity notificationEntityRef = manager.getReference(OfferNotificationEntity.class, onrm.getReference());
		
		if (notificationEntityRef.getAcknowledgementDateTime() != null) {
			
			//TODO throw exception?
		}
		notificationEntityRef.setAcknowledgementDateTime(onrm.getHeader().getCreationDateTime());

	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void notifyRespond(OfferNotificationResponseMessage onrm) {

		TypedQuery<OfferNotificationEntity> query = manager.createNamedQuery("OfferNotificationEntity.findByOfferAndAwarderNumber",
																				OfferNotificationEntity.class);
		
		GlobalCouponNumberEntity offerNumberEntity = gcnEJB.findEntityByNumber(onrm.getOfferNumber());
		GlobalLocationNumberEntity awarderNumberEntity = glnEJB.findEntityByNumber(onrm.getAwarderNumber());
		query.setParameter("offerNumber", offerNumberEntity);
		query.setParameter("awarderNumber", awarderNumberEntity);
		OfferNotificationEntity notificationEntity = query.getSingleResult();
		
		if (notificationEntity.getResponseDateTime() != null) {
			
			//TODO throw exception?
		}
		notificationEntity.setResponseDateTime(onrm.getHeader().getCreationDateTime());
		notificationEntity.setResponseCode(onrm.getResponseCode());
		
		if (onrm.getAwarderClearingAgentNumber() != null) {
			
			AwarderDetailEntity adEntity = adEJB.findEntityByCouponAndAwarderNumber(offerNumberEntity,
																					awarderNumberEntity);
			adEntity.setAwarderClearingAgentNumber(glnEJB.findEntityByNumber(onrm.getAwarderClearingAgentNumber()));
		}
	}

}