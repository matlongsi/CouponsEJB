package com.coupon.common.session;

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

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.UsageConditionEntity;
import com.coupon.common.bean.UsageConditionBean;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.UsageConditionsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=UsageConditionsRemote.NAME, mappedName=UsageConditionsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class UsageConditions implements UsageConditionsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;
	
	@EJB private Offers oEJB;

	static final Logger logger = Logger.getLogger(UsageConditions.class.getName());

	public UsageConditions() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public UsageConditionBean find(long ucId) {

		UsageConditionEntity ucEnt = manager.find(UsageConditionEntity.class, ucId);		
	  	if (ucEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("UsageCondition resource with id '%d' not found", ucId));
	  	}

		return new UsageConditionBean().init(ucEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public UsageConditionEntity createEntity(UsageConditionBean uc) {

		UsageConditionEntity ucEnt = new UsageConditionEntity();
		ucEnt.setMaximumCumulativeUse(uc.getMaximumCumulativeUse());
		ucEnt.setMaximumUsePerTransaction(uc.getMaximumUsePerTransaction());

		return ucEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public UsageConditionBean create(UsageConditionBean uc) {

		UsageConditionEntity ucEnt = createEntity(uc);
		OfferEntity oEnt = oEJB.findEntity(uc.getParentId());
		ucEnt.setOffer(oEnt);

      	try {
      		manager.persist(ucEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {
      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_USAGE_CONDITION")) {
	    			throw new ResourceConflictException(
	    	  				String.format("UsageCondition already exists for Offer number '%s'",
	    	  						oEnt.getOfferNumber()));
      		}
      		throw ex;
      	}
		
		return new UsageConditionBean().init(ucEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public UsageConditionEntity updateEntity(UsageConditionBean uc) {

		UsageConditionEntity ucEntRef;
		try {

			ucEntRef = manager.getReference(UsageConditionEntity.class, uc.getId());
			ucEntRef.setMaximumCumulativeUse(uc.getMaximumCumulativeUse());
			ucEntRef.setMaximumUsePerTransaction(uc.getMaximumUsePerTransaction());
			manager.merge(ucEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("UsageCondition resource for id '%d' not found", uc.getId()));
		}
		
		return ucEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public UsageConditionBean update(UsageConditionBean uc) {

		return new UsageConditionBean().init(updateEntity(uc));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long ucId) {

		try {
			UsageConditionEntity ucEntRef = manager.getReference(UsageConditionEntity.class, ucId);
	      	manager.remove(ucEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("UsageCondition resource for id '%d' not found", ucId));
		}
	}

}