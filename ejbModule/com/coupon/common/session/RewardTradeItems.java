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

import com.coupon.common.bean.RewardTradeItemBean;
import com.coupon.common.entity.RewardTradeItemEntity;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.RewardTradeItemsRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=RewardTradeItemsRemote.NAME, mappedName=RewardTradeItemsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RewardTradeItems implements RewardTradeItemsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private Rewards rEJB;
	@EJB private GlobalTradeIdentificationNumbers gtinEJB;

	static final Logger logger = Logger.getLogger(RewardTradeItems.class.getName());

	public RewardTradeItems() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardTradeItemBean find(long rtiId) {

		RewardTradeItemEntity rtiEnt = manager.find(RewardTradeItemEntity.class, rtiId);		
	  	if (rtiEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("RewardTradeItem resource with id '%d' not found", rtiId));
	  	}

		return new RewardTradeItemBean().init(rtiEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardTradeItemEntity createEntity(RewardTradeItemBean rti) {

		RewardTradeItemEntity rtiEnt = new RewardTradeItemEntity();
		rtiEnt.setTradeItemNumber(gtinEJB.findEntityByNumber(rti.getTradeItemNumber()));
		rtiEnt.setTradeItemQuantity(rti.getTradeItemQuantity());
		
		return rtiEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardTradeItemBean create(RewardTradeItemBean rti) {

		RewardTradeItemEntity rtiEnt = createEntity(rti);
		rtiEnt.setReward(rEJB.findEntity(rti.getParentId()));

		try {
			manager.persist(rtiEnt);
			manager.flush();
		}
	  	catch (PersistenceException ex) {
	
	  		ConstraintViolationException cvx = ExceptionHelper
	  				.unrollException(ex, ConstraintViolationException.class);
	  		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_REWARD_TRADE_ITEM")) {
	    			throw new ResourceConflictException(
	    	  				String.format("RewardTradeItem for trade item number '%s' already exists",
	    	  						rti.getTradeItemNumber().toString()));
	  		}
	  		throw ex;
	  	}
		
		return new RewardTradeItemBean().init(rtiEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardTradeItemEntity updateEntity(RewardTradeItemBean rti) {
		
		RewardTradeItemEntity rtiEntRef;
		try {

			rtiEntRef = manager.getReference(RewardTradeItemEntity.class, rti.getId());
			rtiEntRef.setTradeItemNumber(gtinEJB.findEntityByNumber(rti.getTradeItemNumber()));
			rtiEntRef.setTradeItemQuantity(rti.getTradeItemQuantity());
			manager.merge(rtiEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RewardTradeItem resource for id '%d' not found", rti.getId()));
		}
	  	catch (PersistenceException ex) {
	
	  		ConstraintViolationException cvx = ExceptionHelper
	  				.unrollException(ex, ConstraintViolationException.class);
	  		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_REWARD_TRADE_ITEM")) {
	    			throw new ResourceConflictException(
	    	  				String.format("RewardTradeItem for trade item number '%s' already exists",
	    	  						rti.getTradeItemNumber().toString()));
	  		}
	  		throw ex;
	  	}

		return rtiEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardTradeItemBean update(RewardTradeItemBean rti) {

		return new RewardTradeItemBean().init(updateEntity(rti));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long rtiId) {
      	
		try {
			RewardTradeItemEntity rtiEntRef = manager.getReference(RewardTradeItemEntity.class, rtiId);
	      	manager.remove(rtiEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("RewardTradeItem resource for id '%d' not found", rtiId));
		}
	}

}