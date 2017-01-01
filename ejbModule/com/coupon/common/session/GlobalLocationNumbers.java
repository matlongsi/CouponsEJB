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

import com.coupon.common.GlobalLocationNumber;
import com.coupon.common.entity.GlobalLocationNumberEntity;
import com.coupon.common.bean.GlobalLocationNumberBean;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.GlobalLocationNumbersRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=GlobalLocationNumbersRemote.NAME, mappedName=GlobalLocationNumbersRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class GlobalLocationNumbers implements GlobalLocationNumbersRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	static final Logger logger = Logger.getLogger(GlobalLocationNumbers.class.getName());

	public GlobalLocationNumbers() {
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public List<GlobalLocationNumberBean> findPage(
										long companyPrefix,
										int start,
										int size) {

  		CriteriaBuilder cBuilder = manager.getCriteriaBuilder();
  		CriteriaQuery<GlobalLocationNumberEntity> cQuery = cBuilder.createQuery(GlobalLocationNumberEntity.class);
  		Root<GlobalLocationNumberEntity> glnEnt = cQuery.from(GlobalLocationNumberEntity.class);
  		if (companyPrefix > 0) {
	  		cQuery.where(cBuilder.equal(glnEnt.get("companyPrefix"), companyPrefix));
		}
  		
  		TypedQuery<GlobalLocationNumberEntity> tQuery = manager.createQuery(cQuery);
		tQuery.setFirstResult(start); 		// offset
	    tQuery.setMaxResults(size); 			// limit

	  	List<GlobalLocationNumberBean> glns = new ArrayList<GlobalLocationNumberBean>();
	  	for (GlobalLocationNumberEntity gln : tQuery.getResultList()) {
	  		glns.add(new GlobalLocationNumberBean().init(gln));
	  	}
		if (glns.size() == 0) {
			throw new ResourceNotFoundException(
					String.format("No GlobalLocationNumber resources found%s%s",
							(companyPrefix == 0) ? "" : String.format(" for companyPrefix '%d'", companyPrefix),
							(start == 0) ? "" : String.format(" and page '%d'", start + 1)));
	  	}

		return glns;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalLocationNumberEntity findEntity(long glnId) {

		GlobalLocationNumberEntity glnEnt = manager
				.find(GlobalLocationNumberEntity.class, glnId);
	  	if (glnEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalLocationNumber resource with id '%d' not found", glnId));
	  	}

		return glnEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalLocationNumberBean find(long glnId) {

		return new GlobalLocationNumberBean().init(findEntity(glnId));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalLocationNumberEntity findEntityByNumber(GlobalLocationNumber gln) {

		TypedQuery<GlobalLocationNumberEntity> query = manager.createNamedQuery("GlobalLocationNumberEntity.findByNumber",
																GlobalLocationNumberEntity.class);
		query.setParameter("companyPrefix", gln.getCompanyPrefix());
		query.setParameter("locationReference", gln.getLocationReference());

		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalLocationNumber resource for number '%s' not found", gln.toString()));
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalLocationNumberBean findByNumber(GlobalLocationNumber gln) {

		return new GlobalLocationNumberBean().init(findEntityByNumber(gln));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalLocationNumberBean create(GlobalLocationNumber gln) {

      	GlobalLocationNumberEntity glnEnt = new GlobalLocationNumberEntity();
      	glnEnt.setCompanyPrefix(gln.getCompanyPrefix());
      	glnEnt.setLocationReference(gln.getLocationReference());
      	glnEnt.setCheckDigit(gln.getCheckDigit());

      	try {
      		manager.persist(glnEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {
      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GLN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalLocationNumber resource '%s' already exists", gln.toString()));
      		}
      		throw ex;
      	}

		return new GlobalLocationNumberBean().init(glnEnt);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalLocationNumberBean update(GlobalLocationNumberBean gln) {

		if (gln.getId() == 0) {
			throw new DataValidationException("glnId is required.");
		}

		GlobalLocationNumberEntity glnEntRef;
		try {
			glnEntRef = manager.getReference(GlobalLocationNumberEntity.class, gln.getId());
	      	glnEntRef.setCompanyPrefix(gln.getCompanyPrefix());
	      	glnEntRef.setLocationReference(gln.getLocationReference());
	      	glnEntRef.setCheckDigit(gln.getCheckDigit());
			manager.merge(glnEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalLocationNumber resource for id '%d' not found", gln.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GLN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalLocationNumber resource '%s' already exists", gln.toString()));
      		}
      		throw ex;
      	}

		return new GlobalLocationNumberBean().init(glnEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long glnId) {

		try {
	      	GlobalLocationNumberEntity glnEntRef = manager.getReference(GlobalLocationNumberEntity.class, glnId);
	      	manager.remove(glnEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalLocationNumber resource for id '%d' not found", glnId));
		}
	}

}