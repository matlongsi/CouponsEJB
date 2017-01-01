package com.coupon.common.session.remote;

import java.util.List;

import javax.ejb.Remote;

import com.coupon.common.GlobalServiceRelationNumber;
import com.coupon.common.bean.GlobalServiceRelationNumberBean;


@Remote
public interface GlobalServiceRelationNumbersRemote {

	public static final String NAME = "GlobalServiceRelationNumbers";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/GlobalServiceRelationNumbers";
	
	public List<GlobalServiceRelationNumberBean> findPage(
												long companyPrefix,
												int start,
												int size);

	public GlobalServiceRelationNumberBean find(long gsrnId);

	public GlobalServiceRelationNumberBean findByNumber(GlobalServiceRelationNumber gsrn);

	public GlobalServiceRelationNumberBean create(GlobalServiceRelationNumber gsrn);

	public GlobalServiceRelationNumberBean update(GlobalServiceRelationNumberBean gsrn);

	public void delete(long gsrnId);

}