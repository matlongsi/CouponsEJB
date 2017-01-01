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

import com.coupon.common.bean.PurchaseTradeItemBean;
import com.coupon.common.entity.PurchaseTradeItemEntity;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.PurchaseTradeItemsRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=PurchaseTradeItemsRemote.NAME, mappedName=PurchaseTradeItemsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class PurchaseTradeItems implements PurchaseTradeItemsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private PurchaseRequirements prEJB;
	@EJB private GlobalTradeIdentificationNumbers gtinEJB;
	
	static final Logger logger = Logger.getLogger(PurchaseTradeItems.class.getName());

	public PurchaseTradeItems() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public PurchaseTradeItemBean find(long ptiId) {

		PurchaseTradeItemEntity ptiEnt = manager.find(PurchaseTradeItemEntity.class, ptiId);		
	  	if (ptiEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("PurchaseTradeItem resource with id '%d' not found", ptiId));
	  	}

		return new PurchaseTradeItemBean().init(ptiEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public PurchaseTradeItemEntity createEntity(PurchaseTradeItemBean pti) {

		PurchaseTradeItemEntity ptiEnt = new PurchaseTradeItemEntity();		
		ptiEnt.setTradeItemNumber(gtinEJB.findEntityByNumber(pti.getTradeItemNumber()));
		ptiEnt.setTradeItemGroup(pti.getTradeItemGroup());
		ptiEnt.setTradeItemQuantity(pti.getTradeItemQuantity());

		return ptiEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public PurchaseTradeItemBean create(PurchaseTradeItemBean pti) {

		PurchaseTradeItemEntity ptiEnt = createEntity(pti);
		ptiEnt.setPurchaseRequirement(prEJB.findEntity(pti.getParentId()));
		
		try {
		    manager.persist(ptiEnt);
		    manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_PURCHASE_TRADE_ITEM")) {
	    			throw new ResourceConflictException(
	    	  				String.format("PurchaseTradeItem for trade item number '%s' already exists",
	    	  						pti.getTradeItemNumber().toString()));
      		}
      		throw ex;
      	}
		
		return new PurchaseTradeItemBean().init(ptiEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public PurchaseTradeItemEntity updateEntity(PurchaseTradeItemBean pti) {
		
		PurchaseTradeItemEntity ptiEntRef;
		try {

			ptiEntRef = manager.getReference(PurchaseTradeItemEntity.class, pti.getId());
			ptiEntRef.setTradeItemNumber(gtinEJB.findEntityByNumber(pti.getTradeItemNumber()));
			ptiEntRef.setTradeItemGroup(pti.getTradeItemGroup());
			ptiEntRef.setTradeItemQuantity(pti.getTradeItemQuantity());
			manager.merge(ptiEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("PurchaseTradeItem resource for id '%d' not found", pti.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_PURCHASE_TRADE_ITEM")) {
	    			throw new ResourceConflictException(
	    	  				String.format("PurchaseTradeItem for trade item number '%s' already exists",
	    	  						pti.getTradeItemNumber().toString()));
      		}
      		throw ex;
      	}

		return ptiEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public PurchaseTradeItemBean update(PurchaseTradeItemBean pti) {

		return new PurchaseTradeItemBean().init(updateEntity(pti));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long ptiId) {

		try {
			PurchaseTradeItemEntity ptiEntRef = manager.getReference(PurchaseTradeItemEntity.class, ptiId);
	      	manager.remove(ptiEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("PurchaseTradeItem resource for id '%d' not found", ptiId));
		}
	}

}