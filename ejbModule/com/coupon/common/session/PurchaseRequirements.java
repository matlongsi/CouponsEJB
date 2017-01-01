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
import com.coupon.common.entity.PurchaseRequirementEntity;
import com.coupon.common.entity.PurchaseTradeItemEntity;
import com.coupon.common.bean.PurchaseRequirementBean;
import com.coupon.common.bean.PurchaseTradeItemBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.PurchaseRequirementsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=PurchaseRequirementsRemote.NAME, mappedName=PurchaseRequirementsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class PurchaseRequirements implements PurchaseRequirementsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private PurchaseTradeItems prtiEJB;
	@EJB private Offers oEJB;
	
	static final Logger logger = Logger.getLogger(PurchaseRequirements.class.getName());

	public PurchaseRequirements() {
	}

	public void validate(PurchaseRequirementEntity pre) throws DataValidationException {
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public PurchaseRequirementEntity findEntity(long prId) {

		PurchaseRequirementEntity prEnt = manager.find(PurchaseRequirementEntity.class, prId);		
	  	if (prEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("PurchaseRequirement resource with id '%d' not found", prId));
	  	}

		return prEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public PurchaseRequirementBean find(long prId) {

		return new PurchaseRequirementBean().init(findEntity(prId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public PurchaseRequirementEntity createEntity(PurchaseRequirementBean pr) {

		PurchaseRequirementEntity prEnt = new PurchaseRequirementEntity();

		prEnt.setPurchaseRequirementType(pr.getPurchaseRequirementType());
		switch (prEnt.getPurchaseRequirementType()) {
	    	case SPECIFIED_PURCHASE_AMOUNT:
	    		prEnt.setPurchaseMonetaryAmount(pr.getPurchaseMonetaryAmount());
	    		break;
	    		
	    	case ONE_ITEM_PER_GROUP:
	    	case ONE_OF_SPECIFIED_ITEMS:
	    	case ALL_SPECIFIED_ITEMS:
	    		Map<GlobalTradeIdentificationNumber, PurchaseTradeItemEntity> ptiMap = 
	    				new HashMap<GlobalTradeIdentificationNumber, PurchaseTradeItemEntity>();
	    		for (PurchaseTradeItemBean pti : pr.getPurchaseTradeItems()) {

	    			PurchaseTradeItemEntity ptiEntity = prtiEJB.createEntity(pti);
	    			ptiEntity.setPurchaseRequirement(prEnt);
	    			ptiMap.put(pti.getTradeItemNumber(), ptiEntity);
	    		}
	    		prEnt.setPurchaseTradeItemsMap(ptiMap);
	    		break;
	    		
		default:
			break;
    		}

		return prEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public PurchaseRequirementBean create(PurchaseRequirementBean pr) {

		PurchaseRequirementEntity prEnt = createEntity(pr);
		OfferEntity oEnt = oEJB.findEntity(pr.getParentId());
		prEnt.setOffer(oEnt);
		validate(prEnt);

      	try {
      		manager.persist(prEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_PURCHASE_REQUIREMENT")) {
	    			throw new ResourceConflictException(
	    	  				String.format("PurchaseRequirement for Offer number '%s' already exists",
	    	  						oEnt.getOfferNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new PurchaseRequirementBean().init(prEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public PurchaseRequirementEntity updateEntity(PurchaseRequirementBean pr) {

		PurchaseRequirementEntity prEntRef = null;
		try {

			prEntRef = manager.getReference(PurchaseRequirementEntity.class, pr.getId());
			prEntRef.setPurchaseRequirementType(pr.getPurchaseRequirementType());
			switch (prEntRef.getPurchaseRequirementType()) {
		    	case SPECIFIED_PURCHASE_AMOUNT:
		    		if (prEntRef.getPurchaseTradeItemsMap().size() > 0) {
		    			throw new ValidationException("PurchaseRequirementTradeItem(s) need to be removed before changing purchaseRequirementType.");
		    		}
		    		prEntRef.setPurchaseMonetaryAmount(pr.getPurchaseMonetaryAmount());
		    		break;
		    		
		    	case ONE_ITEM_PER_GROUP:
		    	case ONE_OF_SPECIFIED_ITEMS:
		    	case ALL_SPECIFIED_ITEMS:
		    		prEntRef.setPurchaseMonetaryAmount(null);
		    		for (PurchaseTradeItemBean pti : pr.getPurchaseTradeItems()) {
	
		    			PurchaseTradeItemEntity ptiEntity = prtiEJB.updateEntity(pti);
		    			prEntRef.getPurchaseTradeItemsMap().put(pti.getTradeItemNumber(), ptiEntity);
		    		}
		    		break;
		    		
			default:
				break;
	    		}
      		manager.persist(prEntRef);
      		manager.flush();
      	}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("PurchaseRequirement resource for id '%d' not found", pr.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_PURCHASE_REQUIREMENT")) {
	    			throw new ResourceConflictException(
	    	  				String.format("PurchaseRequirement for Offer number '%s' already exists",
	    	  						prEntRef.getOffer().getOfferNumber().toString()));
      		}
      		throw ex;
      	}

		return prEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public PurchaseRequirementBean update(PurchaseRequirementBean pr) {

		return new PurchaseRequirementBean().init(updateEntity(pr));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long prId) {
      	
		try {
			PurchaseRequirementEntity prEntRef = manager.getReference(PurchaseRequirementEntity.class, prId);
	      	manager.remove(prEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("PurchaseRequirement resource for id '%d' not found", prId));
		}
	}

}