/*
 * Copyright 2015 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.internal;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmFieldType;
import io.realm.TestHelper;
import io.realm.internal.Table.PivotType;
import io.realm.rule.TestRealmConfigurationFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class PivotTest {

    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();

    private RealmConfiguration config;
    private SharedRealm sharedRealm;

    private Table t;
    private long colIndexSex;
    private long colIndexAge;
    private long colIndexHired;

    @Before
    public void setUp() throws Exception {
        Realm.init(InstrumentationRegistry.getInstrumentation().getContext());
        config = configFactory.createConfiguration();
        sharedRealm = SharedRealm.getInstance(config);

        t = TestHelper.createTable(sharedRealm, "temp", new TestHelper.TableSetup() {
            @Override
            public void execute(Table t) {
                colIndexSex = t.addColumn(RealmFieldType.STRING, "sex");
                colIndexAge = t.addColumn(RealmFieldType.INTEGER, "age");
                colIndexHired = t.addColumn(RealmFieldType.BOOLEAN, "hired");

                for (long i=0;i<50000;i++){
                    String sex = i % 2 == 0 ? "Male" : "Female";
                    TestHelper.addRowWithValues(t, sex, 20 + (i%20), true);
                }
            }
        });
    }

    @After
    public void tearDown() {
        if (sharedRealm != null && !sharedRealm.isClosed()) {
            sharedRealm.close();
        }
    }

    @Test
    public void pivotTable(){

        Table resultCount = t.pivot(colIndexSex, colIndexAge, PivotType.COUNT);
        assertEquals(2, resultCount.size());
        assertEquals(25000, resultCount.getLong(1, 0));
        assertEquals(25000, resultCount.getLong(1, 1));

        Table resultMin = t.pivot(colIndexSex, colIndexAge, PivotType.MIN);
        assertEquals(20, resultMin.getLong(1, 0));
        assertEquals(21, resultMin.getLong(1, 1));

        Table resultMax = t.pivot(colIndexSex, colIndexAge, PivotType.MAX);
        assertEquals(38, resultMax.getLong(1, 0));
        assertEquals(39, resultMax.getLong(1, 1));

        try { t.pivot(colIndexHired, colIndexAge, PivotType.SUM); fail("Group by not a String column"); } catch (UnsupportedOperationException ignore) { }
        try { t.pivot(colIndexSex, colIndexHired, PivotType.SUM); fail("Aggregation not an int column"); } catch (UnsupportedOperationException ignore) { }
    }
}
