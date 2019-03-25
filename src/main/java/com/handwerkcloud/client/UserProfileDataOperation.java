/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.handwerkcloud.client;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.OCSRemoteOperation;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.core.util.Pair;

public class UserProfileDataOperation extends OCSRemoteOperation {
    private static final String TAG = UserProfileDataOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    private static final String PROFILE_DATA_URL = "/ocs/v2.php/apps/handwerkcloud/api/v1/settings";

    private String userID;
    private String fields;

    // JSON node names
    private static final String NODE_OCS = "ocs";
    private static final String NODE_DATA = "data";
    private static final String NODE_URL = "url";
    private static final String JSON_FORMAT = "?format=json";

    public UserProfileDataOperation(String userID, String fields) {
        this.userID = userID;
        this.fields = fields;
    }

    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        if (fields != null) {
            PostMethod postMethod = null;

            try {
                postMethod = new PostMethod(client.getBaseUri() + PROFILE_DATA_URL + "/" + userID + JSON_FORMAT);
                postMethod.setParameter("data", fields);

                // remote request
                postMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

                int status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

                if (status == HttpStatus.SC_OK) {
                    String response = postMethod.getResponseBodyAsString();

                    // Parse the response
                    JSONObject respJSON = new JSONObject(response);
                    JSONObject data = respJSON.getJSONObject(NODE_OCS).getJSONObject(NODE_DATA).getJSONObject(NODE_DATA);

                    result = new RemoteOperationResult(true, postMethod);
                } else {
                    result = new RemoteOperationResult(false, postMethod);
                    client.exhaustResponse(postMethod.getResponseBodyAsStream());
                }
            } catch (Exception e) {
                result = new RemoteOperationResult(e);
                Log_OC.e(TAG, "Set profile data for user with id " + userID + " failed: " + result.getLogMessage(),
                    result.getException());
            } finally {
                if (postMethod != null) {
                    postMethod.releaseConnection();
                }
            }
        }
        else {
            GetMethod getMethod = null;

            try {
                getMethod = new GetMethod(client.getBaseUri() + PROFILE_DATA_URL + "/" + userID + JSON_FORMAT);

                // remote request
                getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

                int status = client.executeMethod(getMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

                if (status == HttpStatus.SC_OK) {
                    String response = getMethod.getResponseBodyAsString();

                    // Parse the response
                    JSONObject respJSON = new JSONObject(response);
                    JSONObject data = respJSON.getJSONObject(NODE_OCS).getJSONObject(NODE_DATA).getJSONObject(NODE_DATA);

                    result = new RemoteOperationResult(true, getMethod);
                    ArrayList<Object> dataArray = new ArrayList<>();
                    dataArray.add(data);
                    result.setData(dataArray);
                } else {
                    result = new RemoteOperationResult(false, getMethod);
                    client.exhaustResponse(getMethod.getResponseBodyAsStream());
                }
            } catch (Exception e) {
                result = new RemoteOperationResult(e);
                Log_OC.e(TAG, "Get profile data for user with id " + userID + " failed: " + result.getLogMessage(),
                    result.getException());
            } finally {
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
            }
        }
        return result;
    }
}
