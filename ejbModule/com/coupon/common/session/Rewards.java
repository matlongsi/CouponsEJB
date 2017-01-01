package com.coupon.common.session;

import java.util.HashMap;
import java.util.Map;
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
import javax.validation.ValidationException;

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.GlobalTradeIdentificationNumber;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.entity.RewardEntity;
import com.coupon.common.entity.RewardLoyaltyPointEntity;
import com.coupon.common.entity.RewardTradeItemEntity;
import com.coupon.common.bean.RewardBean;
import com.coupon.common.bean.RewardLoyaltyPointBean;
import com.coupon.common.bean.RewardTradeItemBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.RewardsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=RewardsRemote.NAME, mappedName=RewardsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class Rewards implements RewardsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private RewardLoyaltyPoints rlpEJB;
	@EJB private RewardTradeItems rtiEJB;
	@EJB private Offers oEJB;
	
	static final Logger logger = Logger.getLogger(Rewards.class.getName());

	public Rewards() {
	}

	public void validate(RewardEntity re) throws DataValidationException {
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public RewardEntity findEntity(long rId) {

		RewardEntity rEnt = manager.find(RewardEntity.class, rId);		
	  	if (rEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("Reward resource with id '%d' not found", rId));
	  	}

		return rEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public RewardBean find(long rId) {

		return new RewardBean().init(findEntity(rId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardEntity createEntity(RewardBean r) {

		RewardEntity rEnt = new RewardEntity();
		rEnt.setRewardType(r.getRewardType());
		switch (rEnt.getRewardType()) {

    		case MONETARY_REWARD:
    			rEnt.setRewardMonetaryAmount(r.getRewardMonetaryAmount());
	    		break;

    		case LOYALTY_POINTS_REWARD:
	    		Map<String, RewardLoyaltyPointEntity> rlpMap = new HashMap<String, RewardLoyaltyPointEntity>();
	    		for (RewardLoyaltyPointBean rlp : r.getRewardLoyaltyPoints()) {

	    			RewardLoyaltyPointEntity rlpEntity = rlpEJB.createEntity(rlp);
	    			rlpEntity.setReward(rEnt);
	    			rlpMap.put(rlpEntity.getLoyaltyProgramName(), rlpEntity);
	    		}
	    		rEnt.setRewardLoyaltyPointsMap(rlpMap);
	    		break;
	    		
	    	case TRADE_ITEM_REWARD:
	    		Map<GlobalTradeIdentificationNumber, RewardTradeItemEntity> rtiMap =
	    				new HashMap<GlobalTradeIdentificationNumber, RewardTradeItemEntity>();
	    		for (RewardTradeItemBean rti : r.getRewardTradeItems()) {

	    			RewardTradeItemEntity rtiEntity = rtiEJB.createEntity(rti);
	    			rtiEntity.setReward(rEnt);
	    			rtiMap.put(rtiEntity.getTradeItemNumber(), rtiEntity);
	    		}
	    		rEnt.setRewardTradeItemsMap(rtiMap);
	    		break;
	    		
		default:
			break;
    		}

		return rEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardBean create(RewardBean r) {

		RewardEntity rEnt = createEntity(r);
		OfferEntity oEnt = oEJB.findEntity(r.getParentId());
		rEnt.setOffer(oEnt);
		validate(rEnt);

      	try {
      		manager.persist(rEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_REWARD")) {
	    			throw new ResourceConflictException(
	    	  				String.format("Reward for Offer number '%s' already exists",
	    	  						oEnt.getOfferNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new RewardBean().init(rEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public RewardEntity updateEntity(RewardBean r) {

		RewardEntity rEntRef = null;
		try {

			rEntRef = manager.getReference(RewardEntity.class, r.getId());
			rEntRef.setRewardType(r.getRewardType());
			switch (rEntRef.getRewardType()) {

			case MONETARY_REWARD:
		    		if (rEntRef.getRewardLoyaltyPointsMap().size() > 0) {
		    			throw new ValidationException("RewardLoyaltyPoint(s) need to be removed before changing rewardType.");
		    		}
		    		if (rEntRef.getRewardTradeItemsMap().size() > 0) {
		    			throw new ValidationException("RewardTradeItem(s) need to be removed before changing rewardType.");
		    		}
		    		rEntRef.setRewardMonetaryAmount(r.getRewardMonetaryAmount());
		    		break;
		
				case LOYALTY_POINTS_REWARD:
		    		if (rEntRef.getRewardTradeItemsMap().size() > 0) {
		    			throw new ValidationException("OfferRewardTradeItem(s) need to be removed before changing rewardType.");
		    		}
		    		for (RewardLoyaltyPointBean rlp : r.getRewardLoyaltyPoints()) {
		
		    			RewardLoyaltyPointEntity rlpEntity = rlpEJB.updateEntity(rlp);
		    			rEntRef.getRewardLoyaltyPointsMap().put(rlpEntity.getLoyaltyProgramName(), rlpEntity);
		    		}
		    		break;
		    		
		    	case TRADE_ITEM_REWARD:
		    		if (rEntRef.getRewardLoyaltyPointsMap().size() > 0) {
		    			throw new ValidationException("OfferRewardLoyaltyPoint(s) need to be removed before changing rewardType.");
		    		}
		    		for (RewardTradeItemBean rti : r.getRewardTradeItems()) {
		
		    			RewardTradeItemEntity rtiEntity = rtiEJB.updateEntity(rti);
		    			rEntRef.getRewardTradeItemsMap().put(rtiEntity.getTradeItemNumber(), rtiEntity);
		    		}
		    		break;
	
			default:
				break;
	    		}
      		manager.persist(rEntRef);
      		manager.flush();
      	}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Reward resource for id '%d' not found", r.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_REWARD")) {
	    			throw new ResourceConflictException(
	    	  				String.format("Reward for Offer number '%s' already exists",
	    	  						rEntRef.getOffer().getOfferNumber().toString()));
      		}
      		throw ex;
      	}

		return rEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public RewardBean update(RewardBean r) {

		RewardEntity rEntRef = updateEntity(r);
		validate(rEntRef);

		return new RewardBean().init(rEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long rId) {

		try {
			RewardEntity rEntRef = manager.getReference(RewardEntity.class, rId);
	      	manager.remove(rEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("Reward resource for id '%d' not found", rId));
		}
	}

}