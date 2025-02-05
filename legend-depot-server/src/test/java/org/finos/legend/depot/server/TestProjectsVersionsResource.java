//  Copyright 2021 Goldman Sachs
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

package org.finos.legend.depot.server;

import org.finos.legend.depot.domain.project.ProjectVersion;
import org.finos.legend.depot.server.resources.ProjectsVersionsResource;
import org.finos.legend.depot.services.TestBaseServices;
import org.finos.legend.depot.services.projects.ProjectsServiceImpl;
import org.finos.legend.depot.store.admin.api.metrics.QueryMetricsStore;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static org.finos.legend.depot.domain.version.VersionValidator.MASTER_SNAPSHOT;
import static org.mockito.Mockito.mock;

public class TestProjectsVersionsResource extends TestBaseServices
{
    private final QueryMetricsStore metrics = mock(QueryMetricsStore.class);
    private ProjectsVersionsResource projectsVersionsResource = new ProjectsVersionsResource(new ProjectsServiceImpl(projectsVersionsStore, projectsStore, metrics));

    @Test
    public void canQueryLatestProjectVersionData()
    {
        Optional<ProjectsVersionsResource.ProjectVersionDTO> versionData = projectsVersionsResource.getProjectVersion("examples.metadata", "test","latest");
        Assert.assertTrue(versionData.isPresent());
        Assert.assertEquals(versionData.get().getGroupId(), "examples.metadata");
        Assert.assertEquals(versionData.get().getArtifactId(), "test");
        Assert.assertEquals(versionData.get().getVersionId(), "2.3.1");
        Assert.assertEquals(versionData.get().getVersionData().getDependencies().get(0), new ProjectVersion("examples.metadata", "test-dependencies", "1.0.0"));

        Optional<ProjectsVersionsResource.ProjectVersionDTO> versionData1 = projectsVersionsResource.getProjectVersion("somethig.random", "test","latest");
        Assert.assertFalse(versionData1.isPresent());
    }

    @Test
    public void canQueryHeadProjectVersionData()
    {
        Optional<ProjectsVersionsResource.ProjectVersionDTO> versionData = projectsVersionsResource.getProjectVersion("examples.metadata", "test","head");
        Assert.assertTrue(versionData.isPresent());
        Assert.assertEquals(versionData.get().getGroupId(), "examples.metadata");
        Assert.assertEquals(versionData.get().getArtifactId(), "test");
        Assert.assertEquals(versionData.get().getVersionId(), MASTER_SNAPSHOT);

        Optional<ProjectsVersionsResource.ProjectVersionDTO> versionData1 = projectsVersionsResource.getProjectVersion("somethig.random", "test","head");
        Assert.assertFalse(versionData1.isPresent());
    }
}
