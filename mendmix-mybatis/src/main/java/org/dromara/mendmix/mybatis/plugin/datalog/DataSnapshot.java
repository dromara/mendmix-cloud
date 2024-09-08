/**
 * 
 */
package org.dromara.mendmix.mybatis.plugin.datalog;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.constants.DataChangeType;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.mybatis.core.BaseEntity;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年12月12日
 */
public class DataSnapshot {
	String changeId;
	String sourceMethod;
	String mapperName;
	List<Object> ids;
	List<? extends BaseEntity> entities;
	DataChangeType changeType;
	
	public DataSnapshot(String sourceMethod,String mapperName,List<Object> ids, List<? extends BaseEntity> entities) {
		this.changeId = GUID.uuid();
		this.sourceMethod = sourceMethod;
		this.mapperName = mapperName;
		this.ids = ids;
		this.entities = entities;
		if(ids == null) {
			this.changeType = DataChangeType.add;
		}else if(entities == null) {
			this.changeType = DataChangeType.delete;
		}else {
			this.changeType = DataChangeType.update;
		}
	}
	
	public String buildSortEventContent() {
		List<Object> ids = this.ids;
		if(ids == null && entities != null) {
			ids = entities.stream().map(BaseEntity::getId).collect(Collectors.toList());
		}
		return new StringBuilder(this.changeType.name())
		        .append(GlobalConstants.COLON)
		        .append(StringUtils.join(ids))
		        .toString();
	}

	@Override
	public String toString() {
		return new StringBuilder(">>EntityListWrapper")
				.append("\n - mapper:").append(sourceMethod)
				.append("\n - id:").append(ids == null ? null : JsonUtils.toJson(ids))
				.toString();
	}
	
	
}
