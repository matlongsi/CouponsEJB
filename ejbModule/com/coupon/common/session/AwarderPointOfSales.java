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

import com.coupon.common.bean.AwarderPointOfSaleBean;
import com.coupon.common.entity.AwarderPointOfSaleEntity;
import com.coupon.common.entity.GlobalLocationNumberEntity;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.AwarderPointOfSalesRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=AwarderPointOfSalesRemote.NAME, mappedName=AwarderPointOfSalesRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class AwarderPointOfSales implements AwarderPointOfSalesRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;
	
	@EJB private GlobalLocationNumbers glnEJB;
	@EJB private AwarderDetails oadEJB;

	static final Logger logger = Logger.getLogger(AwarderPointOfSales.class.getName());

	public AwarderPointOfSales() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AwarderPointOfSaleBean find(long aposId) {

		AwarderPointOfSaleEntity aposEnt = manager.find(AwarderPointOfSaleEntity.class, aposId);		
	  	if (aposEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("AwarderPointOfSale resource with id '%d' not found", aposId));
	  	}

		return new AwarderPointOfSaleBean().init(aposEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AwarderPointOfSaleEntity createEntity(AwarderPointOfSaleBean apos) {

		AwarderPointOfSaleEntity aposEnt = new AwarderPointOfSaleEntity();
		aposEnt.setStoreNumber((apos.getStoreNumber() == null) ?
				null : glnEJB.findEntityByNumber(apos.getStoreNumber()));
		aposEnt.setStoreInternalId(apos.getStoreInternalId());
		
		return aposEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AwarderPointOfSaleBean create(AwarderPointOfSaleBean apos) {

		if (apos.getParentId() == null) {
			throw new DataValidationException("adId is required.");
		}

		AwarderPointOfSaleEntity aposEnt = createEntity(apos);
		aposEnt.setAwarderDetail(oadEJB.findEntity(apos.getParentId()));
		try {
			manager.persist(aposEnt);
			manager.flush();
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_AWARDER_STORE_NUMBER")) {
	    			throw new ResourceConflictException(
	    	  				String.format("AwarderPointOfSale with store number '%s' already exists for awarder number '%s'",
	    	  						apos.getStoreNumber().toString(),
	    	  						aposEnt.getAwarderDetail().getAwarderNumber().toString()));
      		}
      		throw ex;
      	}

		return new AwarderPointOfSaleBean().init(aposEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AwarderPointOfSaleEntity updateEntity(AwarderPointOfSaleBean apos) {
		
		AwarderPointOfSaleEntity aposEntRef;
		GlobalLocationNumberEntity storeNumberEnt = glnEJB.findEntityByNumber(apos.getStoreNumber());
		try {

			aposEntRef = manager.getReference(AwarderPointOfSaleEntity.class, apos.getId());
			aposEntRef.setStoreInternalId(apos.getStoreInternalId());
			aposEntRef.setStoreNumber(storeNumberEnt);
			manager.merge(aposEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AwarderPointOfSale resource for id '%d' not found", apos.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_AWARDER_STORE_NUMBER")) {
	    			throw new ResourceConflictException(
	    	  				String.format("AwarderPointOfSale for store number '%s' already exists", apos.getStoreNumber().toString()));
      		}
      		throw ex;
      	}

		return aposEntRef;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public AwarderPointOfSaleBean update(AwarderPointOfSaleBean apos) {

		if (apos.getId() == null) {
			throw new DataValidationException("aposId is required.");
		}

		return new AwarderPointOfSaleBean().init(updateEntity(apos));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long aposId) {

		try {
			AwarderPointOfSaleEntity aposEntRef = manager.getReference(AwarderPointOfSaleEntity.class, aposId);
	      	manager.remove(aposEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("AwarderPointOfSale resource for id '%d' not found", aposId));
		}
	}

}