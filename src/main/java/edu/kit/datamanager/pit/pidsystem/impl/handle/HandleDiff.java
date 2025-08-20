/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
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

package edu.kit.datamanager.pit.pidsystem.impl.handle;

import edu.kit.datamanager.pit.common.PidUpdateException;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import net.handle.hdllib.HandleValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Given two Value Maps, it splits the values in those which have been added,
 * updated or removed.
 * Using this lists, an update can be applied to the old record, to bring it to
 * the state of the new record.
 */
class HandleDiff {
    private final Collection<HandleValue> toAdd = new ArrayList<>();
    private final Collection<HandleValue> toUpdate = new ArrayList<>();
    private final Collection<HandleValue> toRemove = new ArrayList<>();

    @WithSpan(kind = SpanKind.INTERNAL)
    HandleDiff(
            final Map<Integer, HandleValue> recordOld,
            final Map<Integer, HandleValue> recordNew
    ) throws PidUpdateException {
        for (Map.Entry<Integer, HandleValue> old : recordOld.entrySet()) {
            boolean wasRemoved = !recordNew.containsKey(old.getKey());
            if (wasRemoved) {
                // if a row in the record is not available anymore, we need to delete it
                toRemove.add(old.getValue());
            } else {
                // otherwise, we should go and update it.
                // we could also check for equality, but this is the safe and easy way.
                // (the handlevalue classes can be complicated and we'd have to check their
                // equality implementation)
                toUpdate.add(recordNew.get(old.getKey()));
            }
        }
        for (Map.Entry<Integer, HandleValue> e : recordNew.entrySet()) {
            boolean isNew = !recordOld.containsKey(e.getKey());
            if (isNew) {
                // if there is a record which is not in the oldRecord, we need to add it.
                toAdd.add(e.getValue());
            }
        }

        // runtime testing to avoid messing up record states.
        String exceptionMsg = "DIFF NOT VALID. Type: %s. Value: %s";
        for (HandleValue v : toRemove) {
            boolean valid = recordOld.containsValue(v) && !recordNew.containsKey(v.getIndex());
            if (!valid) {
                String message = String.format(exceptionMsg, "Remove", v.toString());
                throw new PidUpdateException(message);
            }
        }
        for (HandleValue v : toAdd) {
            boolean valid = !recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
            if (!valid) {
                String message = String.format(exceptionMsg, "Add", v);
                throw new PidUpdateException(message);
            }
        }
        for (HandleValue v : toUpdate) {
            boolean valid = recordOld.containsKey(v.getIndex()) && recordNew.containsValue(v);
            if (!valid) {
                String message = String.format(exceptionMsg, "Update", v);
                throw new PidUpdateException(message);
            }
        }
    }

    public HandleValue[] added() {
        return this.toAdd.toArray(new HandleValue[]{});
    }

    public HandleValue[] updated() {
        return this.toUpdate.toArray(new HandleValue[]{});
    }

    public HandleValue[] removed() {
        return this.toRemove.toArray(new HandleValue[]{});
    }
}