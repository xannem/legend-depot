//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package org.finos.legend.depot.server.pure.model.context;

import com.google.inject.PrivateModule;
import org.finos.legend.depot.server.pure.model.context.api.PureModelContextService;
import org.finos.legend.depot.server.pure.model.context.resources.DeprecatedPureModelContextAPIsResource;
import org.finos.legend.depot.server.pure.model.context.resources.PureModelContextResource;
import org.finos.legend.depot.server.pure.model.context.services.PureModelContextServiceImpl;

public class PureModelContextModule extends PrivateModule
{
    @Override
    protected void configure()
    {
        bind(PureModelContextService.class).to(PureModelContextServiceImpl.class);
        expose(PureModelContextService.class);

        bind(PureModelContextResource.class);
        expose(PureModelContextResource.class);

        bind(DeprecatedPureModelContextAPIsResource.class);
        expose(DeprecatedPureModelContextAPIsResource.class);
    }
}
