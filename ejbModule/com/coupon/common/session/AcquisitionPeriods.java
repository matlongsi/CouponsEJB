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

import com.coupon.common.entity.AcquisitionPeriodEntity;
import com.coupon.common.entity.TimePeriodEmbed;
import com.coupon.common.TimePeriod;
import com.coupon.common.bean.AcquisitionPeriodBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.AcquisitionPeriodsRemote;
import com.coupon.common.utils.TimePeriodHelper;

@Remote
@LocalBean
@Stateless(name=AcquisitionPeriodsRemote.NAME, mappedName=AcquisitionPeriodsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class AcquisitionPeriods implements AcquisitionPeriodsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private DistributionDetails ddEJB;
	
	static final Logger logger = Logger.getLogger(AcquisitionPeriods.class.getName());

	public AcquisitionPeriods() {
	}

	public void validate(AcquisitionPeriodEntity ape) throws DataValidationException {

		if (ape.getTimePeriod().getEndDateTime().getTime() <=
				ape.getTimePeriod().getStartDateTime().getTime()) {
			throw new DataValidationException(
					String.format("AcquisitionPeriod endDateTime '%s' is less than equal startDateTime '%s'",
							ape.getTimePeriod().getEndDateTime().toString(),
							ape.getTimePeriod().getStartDateTime().toString()));
		}

		if (!ape.getDistributionDetail().getOffer().getTimePeriod().encloses(ape.getTimePeriod())) {
			throw new DataValidationException(
					String.format("AcquisitionPeriod '%s' is not within Offer time period '%s'",
							ape.getTimePeriod().toString(),
							ape.getDistributionDetail().getOffer().getTimePeriod().toString()));
		}

		for (AcquisitionPeriodEntity apEnt : ape.getDistributionDetail().getAcquisitionPeriods()) {
			if (ape.getId() == apEnt.getId()) {
				continue;
			}
			if (ape.equals(apEnt) || ape.getTimePeriod().encloses(apEnt.getTimePeriod())) {
				throw new DataValidationException(
						String.format("AcquisitionPeriod '%s' encloses time period of existing AcquisitionPeriod '%s'",
								ape.getTimePeriod().toString(),
								apEnt.getTimePeriod().toString()));
			}
			if (apEnt.getTimePeriod().encloses(ape.getTimePeriod())) {
				throw new DataValidationException(
						String.format("AcquisitionPeriod '%s' is within time period of existing AcquisitionPeriod '%s'",
								ape.getTimePeriod().toString(),
								apEnt.getTimePeriod().toString()));
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AcquisitionPeriodBean find(long apId) {

		AcquisitionPeriodEntity apEnt = manager.find(AcquisitionPeriodEntity.class, apId);		
	  	if (apEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("AcquisitionPeriod resource with id '%d' not found", apId));
	  	}

	  	AcquisitionPeriodBean apb = new AcquisitionPeriodBean().init(apEnt);
	  	TimePeriod tp = TimePeriodHelper.toTimeZone(
				apb.getTimePeriod(),
				TimeZone.getTimeZone(ddEJB.findEntity(apb.getParentId()).getOffer().getTimeZone()));
		apb.setTimePeriod(tp);

		return apb;
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AcquisitionPeriodEntity createEntity(AcquisitionPeriodBean ap) {

		AcquisitionPeriodEntity apEnt = new AcquisitionPeriodEntity();
		apEnt.setTimePeriod(new TimePeriodEmbed().init(ap.getTimePeriod()));

		return apEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AcquisitionPeriodBean create(AcquisitionPeriodBean ap) {

		TimePeriod tp = TimePeriodHelper.toDefault(
				ap.getTimePeriod(),
				TimeZone.getTimeZone(ddEJB.findEntity(ap.getParentId()).getOffer().getTimeZone()));
		ap.setTimePeriod(tp);
		AcquisitionPeriodEntity apEnt = createEntity(ap);
		apEnt.setDistributionDetail(ddEJB.findEntity(ap.getParentId()));
		validate(apEnt);

		manager.persist(apEnt);
		manager.flush();
		
		return new AcquisitionPeriodBean().init(apEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AcquisitionPeriodEntity updateEntity(AcquisitionPeriodBean ap) {

		AcquisitionPeriodEntity apEntRef;
		try {

			apEntRef = manager.getReference(AcquisitionPeriodEntity.class, ap.getId());
			apEntRef.setTimePeriod(ap.getTimePeriod());
			validate(apEntRef);
			manager.merge(apEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AcquisitionPeriod resource for id '%d' not found", ap.getId()));
		}

		return apEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AcquisitionPeriodBean update(AcquisitionPeriodBean ap) {

		TimePeriod tp = TimePeriodHelper.toDefault(
				ap.getTimePeriod(),
				TimeZone.getTimeZone(ddEJB.findEntity(ap.getParentId()).getOffer().getTimeZone()));
		ap.setTimePeriod(tp);

		return new AcquisitionPeriodBean().init(updateEntity(ap));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long apId) {

		try {
			AcquisitionPeriodEntity apEntRef = manager.getReference(AcquisitionPeriodEntity.class, apId);
	      	manager.remove(apEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AcquisitionPeriod resource for id '%d' not found", apId));
		}
	}

}