/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.rest;

import com.sun.jersey.api.json.JSONWithPadding;
import org.apache.usergrid.corepersistence.service.StatusService;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classy class class.
 */
@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
    "application/ecmascript", "text/jscript"
} )
public class ApplicationsResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsResource.class);


    public ApplicationsResource() {

        logger.info( "ApplicationsResource initialized" );
    } {

    }

    @RequireSystemAccess
    @DELETE
    @Path( "{applicationId}" )
    public JSONWithPadding clearApplication( @Context UriInfo ui,
                                             @PathParam("applicationId") UUID applicationId,
                                             @QueryParam( "confirmApplicationName" ) String confirmApplicationName,
                                             @QueryParam( "limit" ) int limit,
                                             @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        if(confirmApplicationName == null){
            throw new IllegalArgumentException("please make add a QueryString for confirmApplicationName");
        }

        final UUID jobId = UUIDGenerator.newTimeUUID();

        final EntityManager em =  emf.getEntityManager(applicationId);
        final String name =  em.getApplication().getApplicationName();
        if(!name.toLowerCase().equals(confirmApplicationName.toLowerCase())){
            throw new IllegalArgumentException("confirmApplicationName: " + confirmApplicationName + " does not equal " + name);
        }
        final StatusService statusService = injector.getInstance(StatusService.class);

        final ApiResponse response = createApiResponse();

        response.setAction( "clear application" );

        logger.info( "clearing up application" );


        final Thread delete = new Thread() {

            @Override
            public void run() {
                final AtomicInteger itemsDeleted = new AtomicInteger(0);
                try {
                    management.deleteAllEntities(applicationId, limit)
                        .count()
                        .doOnNext(count -> itemsDeleted.set(count))
                        .doOnNext(count -> {
                            if( count%100 == 0 ){
                                Map<String,Object> map = new LinkedHashMap<>();
                                map.put("count",itemsDeleted.intValue());
                                statusService.setStatus(applicationId, jobId, StatusService.Status.INPROGRESS,map);
                            }
                        })
                        .doOnCompleted(() ->{
                            Map<String,Object> map = new LinkedHashMap<>();
                            map.put("count",itemsDeleted.intValue());
                            statusService.setStatus(applicationId,jobId, StatusService.Status.FAILED,map);
                        })
                        .subscribe();

                } catch ( Exception e ) {
                    Map<String,Object> map = new LinkedHashMap<>();
                    map.put("exception",e);
                    statusService.setStatus(applicationId,jobId, StatusService.Status.FAILED,map).subscribe();
                    logger.error( "Failed to delete appid:"+applicationId + " jobid:"+jobId+" count:"+itemsDeleted, e );
                }
            }
        };

        delete.setName( "Delete for app : "+applicationId + " job: "+jobId );
        delete.setDaemon(true);
        delete.start();

        statusService.setStatus(applicationId,jobId, StatusService.Status.STARTED,new LinkedHashMap<>()).subscribe();

        Map<String,Object> data = new HashMap<>();
        data.put("jobId",jobId);
        response.setData(data);
        response.setSuccess();
        return new JSONWithPadding( response, callback );
    }
    @RequireSystemAccess
    @GET
    @Path( "{applicationId}/job/{jobId}" )
    public JSONWithPadding getStatus( @Context UriInfo ui,
                                             @PathParam("applicationId") UUID applicationId,
                                            @PathParam("jobId") UUID jobId,
                                             @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback ) {
        final StatusService statusService = injector.getInstance(StatusService.class);

        final ApiResponse response = createApiResponse();

        response.setAction( "clear application" );

        StatusService.JobStatus jobStatus = statusService.getStatus(applicationId, jobId).toBlocking().lastOrDefault(null);

        Map<String,Object> data = new HashMap<>();
        data.put("jobId",jobId);
        data.put( "status", jobStatus.getStatus().toString() );
        data.put( "metadata", jobStatus.getData().toString() );
        response.setData(data);
        response.setSuccess();

        return new JSONWithPadding( response, callback );

    }

}