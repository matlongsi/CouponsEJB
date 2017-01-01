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

import com.coupon.common.entity.RewardLoyaltyPointEntity;
import com.coupon.common.bean.RewardLoyaltyPointBean;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.RewardLoyaltyPointsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=RewardLoyaltyPointsRemote.NAME, mappedName=RewardLoyaltyPointsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RewardLoyaltyPoints implements RewardLoyaltyPointsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private Rewards rEJB;

	static final Logger logger = Logger.getLogger(RewardLoyaltyPoints.class.getName());

	public RewardLoyaltyPoints() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardLoyaltyPointBean find(long rlpId) {

		RewardLoyaltyPointEntity rlpEnt = manager.find(RewardLoyaltyPointEntity.class, rlpId);
	  	if (rlpEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("RewardLoyaltyPoint resource with id '%d' not found", rlpId));
	  	}

		return new RewardLoyaltyPointBean().init(rlpEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardLoyaltyPointEntity createEntity(RewardLoyaltyPointBean rlp) {

		RewardLoyaltyPointEntity rlpEnt = new RewardLoyaltyPointEntity();		
		rlpEnt.setLoyaltyProgramName(rlp.getLoyaltyProgramName());
		rlpEnt.setLoyaltyPointsQuantity(rlp.getLoyaltyPointsQuantity());
		
		return rlpEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardLoyaltyPointBean create(RewardLoyaltyPointBean rlp) {

		RewardLoyaltyPointEntity rlpEnt = createEntity(rlp);
		rlpEnt.setReward(rEJB.findEntity(rlp.getParentId()));

		try {
		    manager.persist(rlpEnt);
		    manager.flush();
		} catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_REWARD_LOYALTY_PROGRAM_NAME")) {
	    			throw new ResourceConflictException(
	    	  				String.format("RewardLoyaltyPoint for loyalty program name '%s' already exists",
	    	  						rlp.getLoyaltyProgramName()));
      		}
      		throw ex;
      	}
		
		return new RewardLoyaltyPointBean().init(rlpEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardLoyaltyPointEntity updateEntity(RewardLoyaltyPointBean rlp) {
		
		RewardLoyaltyPointEntity rlpEntRef;
		try {

			rlpEntRef = manager.getReference(RewardLoyaltyPointEntity.class, rlp.getId());
			rlpEntRef.setLoyaltyProgramName(rlp.getLoyaltyProgramName());
			rlpEntRef.setLoyaltyPointsQuantity(rlp.getLoyaltyPointsQuantity());
		    manager.persist(rlpEntRef);
		    manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RewardLoyaltyPoint resource for id '%d' not found", rlp.getId()));
		}
		catch (PersistenceException ex) {

			ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_REWARD_LOYALTY_PROGRAM_NAME")) {
	    			throw new ResourceConflictException(
	    	  				String.format("RewardLoyaltyPoint for loyalty program name '%s' already exists",
	    	  						rlp.getLoyaltyProgramName()));
      		}
      		throw ex;
      	}

		return rlpEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardLoyaltyPointBean update(RewardLoyaltyPointBean rlp) {

		return new RewardLoyaltyPointBean().init(updateEntity(rlp));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long rlpId) {
      	
		try {
			RewardLoyaltyPointEntity rlpEntRef = manager.getReference(RewardLoyaltyPointEntity.class, rlpId);
	      	manager.remove(rlpEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RewardLoyaltyPoint resource for id '%d' not found", rlpId));
		}
	}

}