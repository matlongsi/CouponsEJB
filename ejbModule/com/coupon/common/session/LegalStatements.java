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

import com.coupon.common.bean.LegalStatementBean;
import com.coupon.common.entity.LegalStatementEntity;
import com.coupon.common.exception.ResourceNotFoundException;
import com.coupon.common.session.remote.LegalStatementsRemote;


@Remote
@LocalBean
@Stateless(name=LegalStatementsRemote.NAME, mappedName=LegalStatementsRemote.MAPPED_NAME)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class LegalStatements implements LegalStatementsRemote {

	@PersistenceContext(unitName="CouponsJPAService")
	private EntityManager manager;

	@EJB private MarketingMaterials mmEJB;
	
	static final Logger logger = Logger.getLogger(LegalStatements.class.getName());

	public LegalStatements() {
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public LegalStatementEntity findEntity(long lsId) {

		LegalStatementEntity lsEnt = manager.find(LegalStatementEntity.class, lsId);		
	  	if (lsEnt == null) {
	  		throw new ResourceNotFoundException(
	  				String.format("LegalStatement resource with id '%d' not found", lsId));
	  	}

		return lsEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override public LegalStatementBean find(long lsId) {

		return new LegalStatementBean().init(findEntity(lsId));
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public LegalStatementEntity createEntity(LegalStatementBean ls) {

		LegalStatementEntity lsEnt = new LegalStatementEntity();
		lsEnt.setLegalStatement(ls.getLegalStatement());

		return lsEnt;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public LegalStatementBean create(LegalStatementBean ls) {

		LegalStatementEntity lsEnt = createEntity(ls);
		lsEnt.setMarketingMaterial(mmEJB.findEntity(ls.getParentId()));
		manager.persist(lsEnt);
		manager.flush();
		
		return new LegalStatementBean().init(lsEnt);
	}

	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public LegalStatementEntity updateEntity(LegalStatementBean ls) {

		LegalStatementEntity lsEntRef;
		try {

			lsEntRef = manager.getReference(LegalStatementEntity.class, ls.getId());
			lsEntRef.setLegalStatement(ls.getLegalStatement());
			manager.merge(lsEntRef);
			manager.flush();
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("LegalStatement resource for id '%d' not found", ls.getId()));
		}

		return lsEntRef;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public LegalStatementBean update(LegalStatementBean ls) {

		return new LegalStatementBean().init(updateEntity(ls));
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@Override public void delete(long lsId) {

		try {
			LegalStatementEntity lsEntRef = manager.getReference(LegalStatementEntity.class, lsId);
	      	manager.remove(lsEntRef);
		}
		catch (EntityNotFoundException ex) {
			throw new ResourceNotFoundException(
	  				String.format("LegalStatement resource for id '%d' not found", lsId));
		}
	}

}