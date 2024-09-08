/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.gateway.api;

import java.util.List;

import org.dromara.mendmix.gateway.model.BizSystem;
import org.dromara.mendmix.gateway.model.BizSystemModule;
import org.dromara.mendmix.gateway.model.SubSystem;

/**
 * 
 * 
 * <br>
 * Class Name : SystemMgrApi
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020-10-19
 */
public interface SystemInfoApi {

	BizSystem getSystemMetadata(String identifier);

	List<BizSystemModule> getGlobalModules();

	List<SubSystem> getSubSystems(String identifier);

}
