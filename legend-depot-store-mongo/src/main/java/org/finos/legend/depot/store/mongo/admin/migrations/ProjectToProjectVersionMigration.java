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

package org.finos.legend.depot.store.mongo.admin.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.finos.legend.depot.domain.project.ProjectVersion;
import org.finos.legend.depot.domain.project.ProjectVersionData;
import org.finos.legend.depot.domain.project.Property;
import org.finos.legend.depot.domain.project.StoreProjectVersionData;
import org.finos.legend.depot.store.mongo.projects.ProjectsMongo;
import org.finos.legend.depot.store.mongo.projects.ProjectsVersionsMongo;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.finos.legend.depot.domain.version.VersionValidator.MASTER_SNAPSHOT;
import static org.finos.legend.depot.store.mongo.core.BaseMongo.ARTIFACT_ID;
import static org.finos.legend.depot.store.mongo.core.BaseMongo.GROUP_ID;
import static org.finos.legend.depot.store.mongo.core.BaseMongo.VERSION_ID;
import static org.finos.legend.depot.store.mongo.core.BaseMongo.buildDocument;
import static org.finos.legend.depot.store.mongo.projects.ProjectsMongo.PROJECT_ID;

@Deprecated
public final class ProjectToProjectVersionMigration
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ProjectToProjectVersionMigration.class);
    private final MongoDatabase mongoDatabase;

    public ProjectToProjectVersionMigration(MongoDatabase mongoDatabase)
    {
        this.mongoDatabase = mongoDatabase;
    }

    private StoreProjectVersionData createStoreProjectData(Document document, String version)
    {
        String groupId = document.getString(GROUP_ID);
        String artifactId = document.getString(ARTIFACT_ID);
        List<Document> dependenciesDocs = document.getList("dependencies",Document.class, Collections.emptyList());
        List<ProjectVersion> dependencies = dependenciesDocs.stream().filter(doc -> doc.getString(VERSION_ID).equals(version)).map(dep ->
        {
            Document dp = (Document) dep.get("dependency");
            return new ProjectVersion(dp.getString(GROUP_ID),dp.getString(ARTIFACT_ID),dp.getString(VERSION_ID));
        }).collect(Collectors.toList());
        List<Document> propertyDocs = document.getList("properties",Document.class,Collections.emptyList());
        List<Property> properties = propertyDocs.stream().filter(doc -> doc.getString("projectVersionId").equals(version)).map(property ->
                new Property(property.getString("propertyName"),property.getString("value"))).collect(Collectors.toList());
        return new StoreProjectVersionData(groupId, artifactId, version, false, new ProjectVersionData(dependencies, properties));
    }

    @Deprecated
    public void migrationToProjectVersions()
    {
        mongoDatabase.getCollection(ProjectsVersionsMongo.COLLECTION).drop();
        MongoCollection<Document> projectCollection = mongoDatabase.getCollection(ProjectsMongo.COLLECTION);
        MongoCollection<Document> versionCollection = mongoDatabase.getCollection(ProjectsVersionsMongo.COLLECTION);
        projectCollection.find().forEach((Consumer<Document>) document ->
        {
            AtomicInteger i = new AtomicInteger();
            String groupId = document.getString(GROUP_ID);
            String artifactId = document.getString(ARTIFACT_ID);
            try
            {
                List<String> versions = document.getList("versions",String.class);

                LOGGER.info(String.format("versions that should be inserted [%s]",versions.size()));
                versionCollection.insertOne(buildDocument(createStoreProjectData(document, MASTER_SNAPSHOT)));
                LOGGER.info(String.format("%s-%s-%s insertion completed",groupId,artifactId, MASTER_SNAPSHOT));

                versions.forEach(version ->
                {
                    versionCollection.insertOne(buildDocument(createStoreProjectData(document, version)));
                    LOGGER.info(String.format("%s-%s-%s insertion completed",groupId, artifactId, version));
                    i.incrementAndGet();
                });
                LOGGER.info(String.format("versions inserted [%s]",i.get()));
            }
            catch (Exception e)
            {
                LOGGER.info("Error while inserting data:" + e.getMessage());
                LOGGER.info(String.format("versions inserted [%s] before error",i.get()));
                LOGGER.info(String.format("%s-%s insertion could not be completed",groupId, artifactId));
            }
        });
    }

    @Deprecated
    public void cleanUpProjectData()
    {
        MongoCollection<Document> projectCollection = mongoDatabase.getCollection(ProjectsMongo.COLLECTION);
        AtomicInteger i = new AtomicInteger();
        projectCollection.find().forEach((Consumer<Document>) document ->
        {
            String groupId = document.getString(GROUP_ID);
            String artifactId = document.getString(ARTIFACT_ID);
            try
            {
                projectCollection
                        .updateOne(and(eq(GROUP_ID, groupId),
                                eq(ARTIFACT_ID, artifactId)),
                                Updates.combine(Updates.unset("versions"), Updates.unset("dependencies"), Updates.unset("properties"), Updates.unset("latestVersion")));
                i.incrementAndGet();
                LOGGER.info(String.format("%s-%s updation completed", groupId, artifactId));
            }
            catch (Exception e)
            {
                LOGGER.info("Error while updating data: " + e);

                LOGGER.info(String.format("versions updated [%s] before error", i.get()));
                LOGGER.info(String.format("%s-%s updated could not be completed", groupId, artifactId));
            }
        });
        LOGGER.info(String.format("versions updated [%s]", i.get()));
    }
}
