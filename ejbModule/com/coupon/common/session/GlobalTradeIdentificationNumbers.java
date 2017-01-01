package com.coupon.common.session;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.exception.ConstraintViolationException;

import com.coupon.common.GlobalTradeIdentificationNumber;
import com.coupon.common.entity.GlobalTradeIdentificationNumberEntity;
import com.coupon.common.bean.GlobalTradeIdentificationNumberBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.GlobalTradeIdentificationNumbersRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=GlobalTradeIdentificationNumbersRemote.NAME, mappedName=GlobalTradeIdentificationNumbersRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class GlobalTradeIdentificationNumbers implements GlobalTradeIdentificationNumbersRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	static final Logger logger = Logger.getLogger(GlobalTradeIdentificationNumbers.class.getName());

	public GlobalTradeIdentificationNumbers() {
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public List<GlobalTradeIdentificationNumberBean> findPage(
													long companyPrefix,
													int start,
													int size) {

  		CriteriaBuilder cBuilder = manager.getCriteriaBuilder();
  		CriteriaQuery<GlobalTradeIdentificationNumberEntity> cQuery = cBuilder.createQuery(GlobalTradeIdentificationNumberEntity.class);
  		Root<GlobalTradeIdentificationNumberEntity> gtinEnt = cQuery.from(GlobalTradeIdentificationNumberEntity.class);
  		if (companyPrefix > 0) {
	  		cQuery.where(cBuilder.equal(gtinEnt.get("companyPrefix"), companyPrefix));
		}
  		
  		TypedQuery<GlobalTradeIdentificationNumberEntity> tQuery = manager.createQuery(cQuery);
		tQuery.setFirstResult(start); 		// offset
	    tQuery.setMaxResults(size); 		// limit

	  	List<GlobalTradeIdentificationNumberBean> gtins = new ArrayList<GlobalTradeIdentificationNumberBean>();
	  	for (GlobalTradeIdentificationNumberEntity gtin : tQuery.getResultList()) {
	  		gtins.add(new GlobalTradeIdentificationNumberBean().init(gtin));
	  	}
		if (gtins.size() == 0) {
			throw new ResourceNotFoundException(
					String.format("No GlobalTradeIdentificationNumber resources found%s%s",
							(companyPrefix == 0) ? "" : String.format(" for companyPrefix '%d'", companyPrefix),
							(start == 0) ? "" : String.format(" and page '%d'", start + 1)));
	  	}

		return gtins;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalTradeIdentificationNumberEntity findEntity(long gtinId) {

		GlobalTradeIdentificationNumberEntity gtinEnt = manager
				.find(GlobalTradeIdentificationNumberEntity.class, gtinId);		
	  	if (gtinEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalTradeIdentificationNumber resouce with id '%d' not found", gtinId));
	  	}

	  	return gtinEnt;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalTradeIdentificationNumberBean find(long gtinId) {

		return new GlobalTradeIdentificationNumberBean().init(findEntity(gtinId));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalTradeIdentificationNumberEntity findEntityByNumber(GlobalTradeIdentificationNumber gtin) {

		TypedQuery<GlobalTradeIdentificationNumberEntity> query = manager.createNamedQuery("GlobalTradeIdentificationNumberEntity.findByNumber",
																			GlobalTradeIdentificationNumberEntity.class);
		query.setParameter("companyPrefix", gtin.getCompanyPrefix());
		query.setParameter("itemReference", gtin.getItemReference());
		
		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalTradeIdentificationNumber resource for number '%s' not found", gtin.toString()));
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalTradeIdentificationNumberBean findByNumber(GlobalTradeIdentificationNumber gtin) {

		return new GlobalTradeIdentificationNumberBean().init(findEntityByNumber(gtin));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalTradeIdentificationNumberBean create(GlobalTradeIdentificationNumber gtin) {

      	GlobalTradeIdentificationNumberEntity gtinEnt = new GlobalTradeIdentificationNumberEntity();
      	gtinEnt.setCompanyPrefix(gtin.getCompanyPrefix());
      	gtinEnt.setItemReference(gtin.getItemReference());
      	gtinEnt.setCheckDigit(gtin.getCheckDigit());

      	try {
          	manager.persist(gtinEnt);
          	manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GTIN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalTradeIdentificationNumber resource '%s' already exists", gtin.toString()));
      		}
      		throw ex;
      	}
		
		return new GlobalTradeIdentificationNumberBean().init(gtinEnt);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalTradeIdentificationNumberBean update(GlobalTradeIdentificationNumberBean gtin) {

		if (gtin.getId() == 0) {
			throw new DataValidationException("gtinId is required.");
		}

		GlobalTradeIdentificationNumberEntity gtinEntRef;
		try {
			gtinEntRef = manager.getReference(GlobalTradeIdentificationNumberEntity.class, gtin.getId());
			gtinEntRef.setCompanyPrefix(gtin.getCompanyPrefix());
			gtinEntRef.setItemReference(gtin.getItemReference());
			gtinEntRef.setCheckDigit(gtin.getCheckDigit());
			manager.merge(gtinEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalTradeIdentificationNumber resource for id '%d' not found", gtin.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GTIN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalTradeIdentificationNumber resource '%s' already exists", gtin.toString()));
      		}
      		throw ex;
      	}
		
		return new GlobalTradeIdentificationNumberBean().init(gtinEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long gtinId) {

		try {
	      	GlobalTradeIdentificationNumberEntity gtinEntRef = manager.getReference(GlobalTradeIdentificationNumberEntity.class, gtinId);
	      	manager.remove(gtinEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalTradeIdentificationNumber resource for id '%d' not found", gtinId));
		}
	}

}