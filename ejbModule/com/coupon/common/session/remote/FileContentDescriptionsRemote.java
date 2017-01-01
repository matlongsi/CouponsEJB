package com.coupon.common.session.remote;

import javax.ejb.Remote;

import com.coupon.common.bean.FileContentDescriptionBean;


@Remote
public interface FileContentDescriptionsRemote {

	public static final String NAME = "FileContentDescriptions";
	public static final String MAPPED_NAME = "java:global/CouponsEJB/FileContentDescriptions";
	
	public FileContentDescriptionBean find(long fcdId);

	public FileContentDescriptionBean create(FileContentDescriptionBean fcd);

	public FileContentDescriptionBean update(FileContentDescriptionBean fcd);

	public void delete(long fcdId);

}