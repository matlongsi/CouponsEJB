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

import com.coupon.common.GlobalServiceRelationNumber;
import com.coupon.common.bean.GlobalServiceRelationNumberBean;
import com.coupon.common.entity.GlobalServiceRelationNumberEntity;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.GlobalServiceRelationNumbersRemote;
import com.coupon.common.utils.ExceptionHelper;

@Remote
@LocalBean
@Stateless(name=GlobalServiceRelationNumbersRemote.NAME, mappedName=GlobalServiceRelationNumbersRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class GlobalServiceRelationNumbers implements GlobalServiceRelationNumbersRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	static final Logger logger = Logger.getLogger(GlobalServiceRelationNumbers.class.getName());
	
	public GlobalServiceRelationNumbers() {
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public List<GlobalServiceRelationNumberBean> findPage(
												long companyPrefix,
												int start,
												int size) {

  		CriteriaBuilder cBuilder = manager.getCriteriaBuilder();
  		CriteriaQuery<GlobalServiceRelationNumberEntity> cQuery = cBuilder.createQuery(GlobalServiceRelationNumberEntity.class);
  		Root<GlobalServiceRelationNumberEntity> gsrnEnt = cQuery.from(GlobalServiceRelationNumberEntity.class);
  		if (companyPrefix > 0) {
	  		cQuery.where(cBuilder.equal(gsrnEnt.get("companyPrefix"), companyPrefix));
		}
  		
  		TypedQuery<GlobalServiceRelationNumberEntity> tQuery = manager.createQuery(cQuery);
		tQuery.setFirstResult(start); 		// offset
	    tQuery.setMaxResults(size); 			// limit

	  	List<GlobalServiceRelationNumberBean> gsrns = new ArrayList<GlobalServiceRelationNumberBean>();
	  	for (GlobalServiceRelationNumberEntity gsrn : tQuery.getResultList()) {
	  		gsrns.add(new GlobalServiceRelationNumberBean().init(gsrn));
	  	}
		if (gsrns.size() == 0) {
			throw new ResourceNotFoundException(
					String.format("No GlobalServiceRelationNumber resources found%s%s",
							(companyPrefix == 0) ? "" : String.format(" for companyPrefix '%d'", companyPrefix),
							(start == 0) ? "" : String.format(" and page '%d'", start + 1)));
	  	}

		return gsrns;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalServiceRelationNumberEntity findEntity(long gsrnId) {

		GlobalServiceRelationNumberEntity gsrnEnt = manager
				.find(GlobalServiceRelationNumberEntity.class, gsrnId);		
	  	if (gsrnEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalServiceRelationNumber resouce with id '%d' not found", gsrnId));
	  	}

		return gsrnEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalServiceRelationNumberBean find(long gsrnId) {

		return new GlobalServiceRelationNumberBean().init(findEntity(gsrnId));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalServiceRelationNumberEntity findEntityByNumber(GlobalServiceRelationNumber gsrn) {

		TypedQuery<GlobalServiceRelationNumberEntity> query = manager.createNamedQuery("GlobalServiceRelationNumberEntity.findByNumber",
																		GlobalServiceRelationNumberEntity.class);
		query.setParameter("companyPrefix", gsrn.getCompanyPrefix());
		query.setParameter("serviceReference", gsrn.getServiceReference());

		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalServiceRelationNumber resource for number '%s' not found", gsrn.toString()));
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalServiceRelationNumberBean findByNumber(GlobalServiceRelationNumber gsrn) {

		return new GlobalServiceRelationNumberBean().init(findEntityByNumber(gsrn));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalServiceRelationNumberBean create(GlobalServiceRelationNumber gsrn) {

      	GlobalServiceRelationNumberEntity gsrnEnt = new GlobalServiceRelationNumberEntity().init(gsrn);
      	try {
      		manager.persist(gsrnEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GSRN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalServiceRelationNumber resource '%s' already exists", gsrn.toString()));
      		}
      		throw ex;
      	}
		
		return new GlobalServiceRelationNumberBean().init(gsrnEnt);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalServiceRelationNumberBean update(GlobalServiceRelationNumberBean gsrn) {

		if (gsrn.getId() == 0) {
			throw new DataValidationException("gsrnId is required.");
		}

		GlobalServiceRelationNumberEntity gsrnEntRef;
		try {
			gsrnEntRef = manager.getReference(GlobalServiceRelationNumberEntity.class, gsrn.getId());
			gsrnEntRef.setCompanyPrefix(gsrn.getCompanyPrefix());
			gsrnEntRef.setServiceReference(gsrn.getServiceReference());
			gsrnEntRef.setCheckDigit(gsrn.getCheckDigit());
			manager.merge(gsrnEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalServiceRelationNumber resource for id '%d' not found", gsrn.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GSRN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalServiceRelationNumber resource '%s' already exists", gsrn.toString()));
      		}
      		throw ex;
      	}
		
		return new GlobalServiceRelationNumberBean().init(gsrnEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long gsrnId) {
		
		try {
	      	GlobalServiceRelationNumberEntity gsrnEntRef = manager.getReference(GlobalServiceRelationNumberEntity.class, gsrnId);
	      	manager.remove(gsrnEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalServiceRelationNumber resource for id '%d' not found", gsrnId));
		}
	}
	
}