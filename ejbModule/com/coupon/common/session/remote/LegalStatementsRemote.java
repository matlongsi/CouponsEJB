package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.LegalStatementBean;


@Remote
public interface LegalStatementsRemote {

	public static final String NAME = "LegalStatements";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/LegalStatements";
	
	public LegalStatementBean find(long lsId);

	public LegalStatementBean create(LegalStatementBean ls);

	public LegalStatementBean update(LegalStatementBean ls);

	public void delete(long lsId);

}