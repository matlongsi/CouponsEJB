package com.coupon.common.session.remote;

import java.util.List;

import javax.ejb.Remote;

import com.coupon.common.GlobalCouponNumber;
import com.coupon.common.bean.GlobalCouponNumberBean;


@Remote
public interface GlobalCouponNumbersRemote {

	public static final String NAME = "GlobalCouponNumbers";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/GlobalCouponNumbers";
	
	public List<GlobalCouponNumberBean> findPage(
										long companyPrefix,
										int start,
										int size);

	public GlobalCouponNumberBean find(long gcnId);

	public GlobalCouponNumberBean findByNumber(GlobalCouponNumber gcn);

	public GlobalCouponNumberBean create(GlobalCouponNumber gcn);

	public GlobalCouponNumberBean update(GlobalCouponNumberBean gcn);

	public void delete(long gcnId);

}