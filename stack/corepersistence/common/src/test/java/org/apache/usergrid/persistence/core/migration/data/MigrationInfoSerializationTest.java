/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.core.migration.data;



import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.guice.TestCommonModule;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.test.UseModules;

import com.google.inject.Inject;
import com.netflix.astyanax.Keyspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith( ITRunner.class )
@UseModules( TestCommonModule.class )
public class MigrationInfoSerializationTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    protected Keyspace keyspace;


    protected ApplicationScope scope;

    @Inject
    protected MigrationInfoSerialization migrationInfoSerialization;



    @Test
    public void basicTest(){

        //test getting nothing works
        final String emptyStatus = migrationInfoSerialization.getStatusMessage();

        assertNull(emptyStatus);

        final int unsavedVersion = migrationInfoSerialization.getVersion();

        assertEquals(0, unsavedVersion);

        final int statusCode = migrationInfoSerialization.getStatusCode();

        assertEquals(0, statusCode);

        //now update them

        final String savedStatus = "I'm a test status";

        migrationInfoSerialization.setStatusMessage( savedStatus );

        final String returnedStatus = migrationInfoSerialization.getStatusMessage();

        assertEquals("Correct status returned", savedStatus, returnedStatus);


        final int savedVersion = 100;

        migrationInfoSerialization.setVersion( savedVersion );

        final int returnedVersion = migrationInfoSerialization.getVersion();

        assertEquals("Correct version returned", savedVersion, returnedVersion);

        final int savedStatusCode = 200;

        migrationInfoSerialization.setStatusCode( savedStatusCode );

        final int returnedStatusCode = migrationInfoSerialization.getStatusCode();

        assertEquals("Status code was set correctly", savedStatusCode, returnedStatusCode);
    }
}
