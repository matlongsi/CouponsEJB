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

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.bean.GlobalCouponNumberBean;
import com.coupon.common.entity.GlobalCouponNumberEntity;
import com.coupon.common.exception.DataValidationException;
import com.coupon.common.exception.ResourceConflictException;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.GlobalCouponNumbersRemote;
import com.coupon.common.utils.ExceptionHelper;


@Remote
@LocalBean
@Stateless(name=GlobalCouponNumbersRemote.NAME, mappedName=GlobalCouponNumbersRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class GlobalCouponNumbers implements GlobalCouponNumbersRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	static final Logger logger = Logger.getLogger(GlobalCouponNumbers.class.getName());
	
	public GlobalCouponNumbers() {
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public List<GlobalCouponNumberBean> findPage(
										long companyPrefix,
										int start,
										int size) {

  		CriteriaBuilder cBuilder = manager.getCriteriaBuilder();
  		CriteriaQuery<GlobalCouponNumberEntity> cQuery = cBuilder.createQuery(GlobalCouponNumberEntity.class);
  		Root<GlobalCouponNumberEntity> gcnEnt = cQuery.from(GlobalCouponNumberEntity.class);
  		if (companyPrefix > 0) {
	  		cQuery.where(cBuilder.equal(gcnEnt.get("companyPrefix"), companyPrefix));
		}
  		
  		TypedQuery<GlobalCouponNumberEntity> tQuery = manager.createQuery(cQuery);
		tQuery.setFirstResult(start); 		// offset
	    tQuery.setMaxResults(size); 			// limit

	  	List<GlobalCouponNumberBean> gcns = new ArrayList<GlobalCouponNumberBean>();
	  	for (GlobalCouponNumberEntity gcn : tQuery.getResultList()) {	  		
	  		gcns.add(new GlobalCouponNumberBean().init(gcn));
	  	}
		if (gcns.size() == 0) {
			throw new ResourceNotFoundException(
					String.format("No GlobalCouponNumber resources found%s%s",
							(companyPrefix == 0) ? "" : String.format(" for companyPrefix '%d'", companyPrefix),
							(start == 0) ? "" : String.format(" and page '%d'", start + 1)));
	  	}

		return gcns;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalCouponNumberEntity findEntity(long gcnId) {

		GlobalCouponNumberEntity gcnEnt = manager
				.find(GlobalCouponNumberEntity.class, gcnId);		
	  	if (gcnEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalCouponNumber resouce with id '%d' not found", gcnId));
	  	}

		return gcnEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalCouponNumberBean find(long gcnId) {

		return new GlobalCouponNumberBean().init(findEntity(gcnId));
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public GlobalCouponNumberEntity findEntityByNumber(GlobalCouponNumber gcn) {

		TypedQuery<GlobalCouponNumberEntity> query = manager.createNamedQuery("GlobalCouponNumberEntity.findByNumber",
																GlobalCouponNumberEntity.class);
		query.setParameter("companyPrefix", gcn.getCompanyPrefix());
		query.setParameter("couponReference", gcn.getCouponReference());
		query.setParameter("serialComponent", gcn.getSerialComponent());
		
		try {
			return query.getSingleResult();
		}
		catch (NoResultException ex) {
	  		throw new ResourceNotFoundException(
	  				String.format("GlobalCouponNumber resource for number '%s' not found", gcn.toString()));
		}
		
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public GlobalCouponNumberBean findByNumber(GlobalCouponNumber gcn) {

		return new GlobalCouponNumberBean().init(findEntityByNumber(gcn));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public GlobalCouponNumberEntity createEntity(GlobalCouponNumber gcn) {

      	GlobalCouponNumberEntity gcnEnt = new GlobalCouponNumberEntity();
		gcnEnt.setCompanyPrefix(gcn.getCompanyPrefix());
		gcnEnt.setCouponReference(gcn.getCouponReference());
		gcnEnt.setSerialComponent(gcn.getSerialComponent());
		gcnEnt.setCheckDigit(gcn.getCheckDigit());

      	return gcnEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalCouponNumberBean create(GlobalCouponNumber gcn) {

      	GlobalCouponNumberEntity gcnEnt = createEntity(gcn);

      	try {
      		manager.persist(gcnEnt);
      		manager.flush();
      	}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GCN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalCouponNumber resource '%s' already exists", gcn.toString()));
      		}
      		throw ex;
      	}

		return new GlobalCouponNumberBean().init(gcnEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public GlobalCouponNumberEntity updateEntity(GlobalCouponNumberBean gcn) {

		GlobalCouponNumberEntity gcnEntRef;
		try {

			gcnEntRef = manager.getReference(GlobalCouponNumberEntity.class, gcn.getId());
			gcnEntRef.setCompanyPrefix(gcn.getCompanyPrefix());
			gcnEntRef.setCouponReference(gcn.getCouponReference());
			gcnEntRef.setSerialComponent(gcn.getSerialComponent());
			gcnEntRef.setCheckDigit(gcn.getCheckDigit());
			manager.merge(gcnEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalCouponNumber resource for id '%d' not found", gcn.getId()));
		}
      	catch (PersistenceException ex) {

      		ConstraintViolationException cvx = ExceptionHelper
      				.unrollException(ex, ConstraintViolationException.class);
      		if ((cvx != null) && cvx.getConstraintName().equals("UNIQUE_GCN")) {
	    			throw new ResourceConflictException(
	    	  				String.format("GlobalCouponNumber resource '%s' already exists", gcn.toString()));
      		}
      		throw ex;
      	}
		
		return gcnEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public GlobalCouponNumberBean update(GlobalCouponNumberBean gcn) {

		if (gcn.getId() == 0) {
			throw new DataValidationException("gcnId is required.");
		}

		GlobalCouponNumberEntity gcnEntRef = updateEntity(gcn);

		return new GlobalCouponNumberBean().init(gcnEntRef);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long gcnId) {
		
		try {
	      	GlobalCouponNumberEntity gcnEntRef = manager.getReference(GlobalCouponNumberEntity.class, gcnId);
	      	manager.remove(gcnEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("GlobalCouponNumber resource for id '%d' not found", gcnId));
		}
	}

}