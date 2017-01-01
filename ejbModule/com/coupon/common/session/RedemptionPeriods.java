package com.coupon.common.session;

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

import com.coupon.common.entity.RedemptionPeriodEntity;
import com.coupon.common.entity.TimePeriodEmbed;
import com.coupon.common.TimePeriod;
import com.coupon.common.bean.RedemptionPeriodBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.RedemptionPeriodsRemote;
import com.coupon.common.utils.TimePeriodHelper;

@Remote
@LocalBean
@Stateless(name=RedemptionPeriodsRemote.NAME, mappedName=RedemptionPeriodsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RedemptionPeriods implements RedemptionPeriodsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private AwarderDetails adEJB;
	
	static final Logger logger = Logger.getLogger(RedemptionPeriods.class.getName());

	public RedemptionPeriods() {
	}
	
	public void validate(RedemptionPeriodEntity rpe) throws DataValidationException {

		if (rpe.getTimePeriod().getEndDateTime().getTime() <=
				rpe.getTimePeriod().getStartDateTime().getTime()) {
			throw new DataValidationException(
					String.format("RedemptionPeriod endDateTime '%s' is less than equal startDateTime '%s'",
							rpe.getTimePeriod().getEndDateTime().toString(),
							rpe.getTimePeriod().getStartDateTime().toString()));
		}

		if (!rpe.getAwarderDetail().getOffer().getTimePeriod().encloses(rpe.getTimePeriod())) {
			throw new DataValidationException(
					String.format("RedemptionPeriod '%s' is not within Offer time period '%s'",
							rpe.getTimePeriod().toString(),
							rpe.getAwarderDetail().getOffer().getTimePeriod().toString()));
		}
		for (RedemptionPeriodEntity rpEnt : rpe.getAwarderDetail().getRedemptionPeriods()) {
			if (rpe.getId() == rpEnt.getId()) {
				continue;
			}
			if (rpe.equals(rpEnt) || rpe.getTimePeriod().encloses(rpEnt.getTimePeriod())) {
				throw new DataValidationException(
						String.format("RedemptionPeriod '%s' encloses time period of existing RedemptionPeriod '%s'",
								rpe.getTimePeriod().toString(),
								rpEnt.getTimePeriod().toString()));
			}
			if (rpEnt.getTimePeriod().encloses(rpe.getTimePeriod())) {
				throw new DataValidationException(
						String.format("RedemptionPeriod '%s' is within time period of existing RedemptionPeriod '%s'",
								rpe.getTimePeriod().toString(),
								rpEnt.getTimePeriod().toString()));
			}
		}
		
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RedemptionPeriodBean find(long rpId) {

		RedemptionPeriodEntity rpEnt = manager.find(RedemptionPeriodEntity.class, rpId);		
	  	if (rpEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("RedemptionPeriod resource with id '%d' not found", rpId));
	  	}

	  	RedemptionPeriodBean rpb = new RedemptionPeriodBean().init(rpEnt);
	  	TimePeriod tp = TimePeriodHelper.toTimeZone(
				rpb.getTimePeriod(),
				TimeZone.getTimeZone(adEJB.findEntity(rpb.getParentId()).getOffer().getTimeZone()));
		rpb.setTimePeriod(tp);

		return rpb;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RedemptionPeriodEntity createEntity(RedemptionPeriodBean rp) {

		RedemptionPeriodEntity rpEnt = new RedemptionPeriodEntity();
		rpEnt.setTimePeriod(new TimePeriodEmbed().init(rp.getTimePeriod()));
		
		return rpEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RedemptionPeriodBean create(RedemptionPeriodBean rp) {

		TimePeriod tp = TimePeriodHelper.toDefault(
				rp.getTimePeriod(),
				TimeZone.getTimeZone(adEJB.findEntity(rp.getParentId()).getOffer().getTimeZone()));
		rp.setTimePeriod(tp);
		RedemptionPeriodEntity rpEnt = createEntity(rp);
		rpEnt.setAwarderDetail(adEJB.findEntity(rp.getParentId()));
		validate(rpEnt);

		manager.persist(rpEnt);
		manager.flush();
		
		return new RedemptionPeriodBean().init(rpEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RedemptionPeriodEntity updateEntity(RedemptionPeriodBean rp) {

		RedemptionPeriodEntity rpEntRef;
		try {

			rpEntRef = manager.getReference(RedemptionPeriodEntity.class, rp.getId());
			rpEntRef.setTimePeriod(rp.getTimePeriod());
			validate(rpEntRef);
			manager.merge(rpEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RedemptionPeriod resource for id '%d' not found", rp.getId()));
		}

		return rpEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RedemptionPeriodBean update(RedemptionPeriodBean rp) {

		TimePeriod tp = TimePeriodHelper.toDefault(
				rp.getTimePeriod(),
				TimeZone.getTimeZone(adEJB.findEntity(rp.getParentId()).getOffer().getTimeZone()));
		rp.setTimePeriod(tp);

		return new RedemptionPeriodBean().init(updateEntity(rp));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long rpId) {

		try {
			RedemptionPeriodEntity rpEntRef = manager.getReference(RedemptionPeriodEntity.class, rpId);
	      	manager.remove(rpEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RedemptionPeriod resource for id '%d' not found", rpId));
		}
	}

}