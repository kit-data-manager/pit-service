/*
 * Adapted from https://git.rwth-aachen.de/nfdi4ing/s-3/s-3-3/metadatahub/-/blob/main/src/main/java/edu/kit/metadatahub/doip/rest/Operations.java
 * 
 * License: Apache 2.0
 */

package edu.kit.datamanager.pit.web.doip;

import net.dona.doip.DoipConstants;

/**
 * Define valid operations for REST interface of DOIP.
 */
public enum DoipOperationId {
    // Basic operations
    OP_HELLO(DoipConstants.OP_HELLO),
    OP_CREATE(DoipConstants.OP_CREATE),
    OP_RETRIEVE(DoipConstants.OP_RETRIEVE),
    OP_UPDATE(DoipConstants.OP_UPDATE),
    OP_DELETE(DoipConstants.OP_DELETE),
    OP_SEARCH(DoipConstants.OP_SEARCH),
    OP_LIST(DoipConstants.OP_LIST_OPERATIONS),
    // Extended operations
    OP_VALIDATE("dev/Validation");

    private final String value;

    DoipOperationId(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DoipOperationId fromValue(String v) {
        for (DoipOperationId c : DoipOperationId.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
