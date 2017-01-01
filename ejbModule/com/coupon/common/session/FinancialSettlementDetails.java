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

import com.coupon.common.bean.FinancialSettlementDetailBean;
import com.coupon.common.entity.FinancialSettlementDetailEntity;
import com.coupon.common.entity.OfferEntity;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.FinancialSettlementDetailsRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=FinancialSettlementDetailsRemote.NAME, mappedName=FinancialSettlementDetailsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class FinancialSettlementDetails implements FinancialSettlementDetailsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;
	
	@EJB private Offers oEJB;

	static final Logger logger = Logger.getLogger(FinancialSettlementDetails.class.getName());

	public FinancialSettlementDetails() {
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public FinancialSettlementDetailBean find(long fsdId) {

		FinancialSettlementDetailEntity fsdEnt = manager.find(FinancialSettlementDetailEntity.class, fsdId);		
	  	if (fsdEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("FinancialSettlementDetail resource with id '%d' not found", fsdId));
	  	}

		return new FinancialSettlementDetailBean().init(fsdEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FinancialSettlementDetailEntity createEntity(FinancialSettlementDetailBean fsd) {

		FinancialSettlementDetailEntity fsdEnt = new FinancialSettlementDetailEntity();
		fsdEnt.setOfferClearingInstruction(fsd.getOfferClearingInstruction());

		return fsdEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public FinancialSettlementDetailBean create(FinancialSettlementDetailBean fsd) {

		if (fsd.getParentId() == null) {
			throw new DataValidationException("offerId is required.");
		}

		FinancialSettlementDetailEntity fsdEnt = createEntity(fsd);
		OfferEntity oEnt = oEJB.findEntity(fsd.getParentId());
		fsdEnt.setOffer(oEnt);

      	try {
      		manager.persist(fsdEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("SINGLE_FINANCIAL_SETTLEMENT_DETAIL")) {
	    			throw new ResourceConflictException(
	    	  				String.format("FinancialSettlementDetail already exists for Offer number '%s'",
	    	  						oEnt.getOfferNumber()));
      		}
      		throw ex;
      	}
		
		return new FinancialSettlementDetailBean().init(fsdEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public FinancialSettlementDetailEntity updateEntity(FinancialSettlementDetailBean fsd) {

		FinancialSettlementDetailEntity fsdEntRef;
		try {

			fsdEntRef = manager.getReference(FinancialSettlementDetailEntity.class, fsd.getId());
			fsdEntRef.setOfferClearingInstruction(fsd.getOfferClearingInstruction());
			manager.merge(fsdEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("FinancialSettlementDetail resource for id '%d' not found", fsd.getId()));
		}
		
		return fsdEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public FinancialSettlementDetailBean update(FinancialSettlementDetailBean fsd) {

		if (fsd.getId() == null) {
			throw new DataValidationException("fsdId is required.");
		}

		return new FinancialSettlementDetailBean().init(updateEntity(fsd));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long fsdId) {

		try {
			FinancialSettlementDetailEntity fsdEntRef = manager.getReference(FinancialSettlementDetailEntity.class, fsdId);
	      	manager.remove(fsdEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("FinancialSettlementDetail resource for id '%d' not found", fsdId));
		}
	}

}