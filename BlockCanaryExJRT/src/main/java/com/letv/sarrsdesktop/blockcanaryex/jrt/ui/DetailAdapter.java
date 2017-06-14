/*
 * Copyright (C) 2015 Square, Inc.
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
package com.letv.sarrsdesktop.blockcanaryex.jrt.ui;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.R;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.ViewPerformanceSampler;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

import static com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo.SEPARATOR;

final class DetailAdapter extends BaseAdapter {

    private static final int TOP_ROW = 0;
    private static final int NORMAL_ROW = 1;

    private boolean[] mFoldings = new boolean[0];

    private BlockInfo mBlockInfo;

    private static final int POSITION_TIME = 1;
    private static final int POSITION_ENV = 2;
    private static final int POSITION_GC = 3;
    private static final int POSITION_VIEW = 4;
    private static final int POSITION_TOP_HEAVY_METHOD = 5;
    private static final int POSITION_TOP_FREQUENT_METHOD = 6;
    private static final int POSITION_HEAVY_METHOD = 7;
    private static final int POSITION_FREQUENT_METHOD = 8;

    private static final String FOLDING_SUFFIX = "…";

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        if (getItemViewType(position) == TOP_ROW) {
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(context).inflate(R.layout.block_canary_ex_ref_top_row, parent, false);
            }
            TextView textView = findById(convertView, R.id.__leak_canary_row_text);
            textView.setText(context.getPackageName());
        } else {
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(context).inflate(R.layout.block_canary_ex_ref_row, parent, false);
            }
            TextView textView = findById(convertView, R.id.__leak_canary_row_text);

            boolean isThreadStackEntry = position == POSITION_FREQUENT_METHOD + 1;
            String element = getItem(position);
            String htmlString = elementToHtmlString(element, position, mFoldings[position]);
            if (isThreadStackEntry && !mFoldings[position]) {
                htmlString += " <font color='#919191'>" + "blocked" + "</font>";
            }
            textView.setText(Html.fromHtml(htmlString));

            DisplayConnectorView connectorView = findById(convertView, R.id.__leak_canary_row_connector);
            connectorView.setType(connectorViewType(position));

            MoreDetailsView moreDetailsView = findById(convertView, R.id.__leak_canary_row_more);
            moreDetailsView.setFolding(mFoldings[position]);
        }

        return convertView;
    }

    private DisplayConnectorView.Type connectorViewType(int position) {
        return (position == 1) ? DisplayConnectorView.Type.START : (
                (position == getCount() - 1) ? DisplayConnectorView.Type.END :
                        DisplayConnectorView.Type.NODE);
    }

    private String elementToHtmlString(String element, int position, boolean folding) {
        if(element == null) {
            return "null";
        }
        String htmlString = element.replaceAll(SEPARATOR, "<br>");

        switch (position) {
            case POSITION_TIME:
                htmlString = String.format("<font color='#f3cf83'>%s</font> ", htmlString);
                break;
            case POSITION_ENV:
                if (folding) {
                    htmlString = htmlString.substring(0, htmlString.indexOf(BlockInfo.KEY_TOTAL_MEMORY)) + FOLDING_SUFFIX;
                }
                htmlString = String.format("<font color='#ffff00'>%s</font> ", htmlString);
                break;
            case POSITION_GC:
                if (folding && htmlString.length() > 600) {
                    htmlString = htmlString.substring(0, 600) + FOLDING_SUFFIX;
                }
                htmlString = String.format("<font color='#ff95ca'>%s</font> ", htmlString);
                break;
            case POSITION_VIEW:
                htmlString = String.format("<font color='#ff00ff'>%s</font> ", htmlString);
                break;
            case POSITION_TOP_HEAVY_METHOD:
                htmlString = String.format("<font color='#4a86e8'>%s</font> ", htmlString);
                break;
            case POSITION_TOP_FREQUENT_METHOD:
                htmlString = String.format("<font color='#c48a47'>%s</font> ", htmlString);
                break;
            case POSITION_HEAVY_METHOD:
                if (folding && htmlString.length() > 600) {
                   htmlString = htmlString.substring(0, 600) + FOLDING_SUFFIX;
                }
                htmlString = String.format("<font color='#00ff00'>%s</font> ", htmlString);
                break;
            case POSITION_FREQUENT_METHOD:
            default:
                if (folding && htmlString.length() > 600) {
                    htmlString = htmlString.substring(0, 600) + FOLDING_SUFFIX;
                }
                htmlString = String.format("<font color='#ffffff'>%s</font> ", htmlString);
                break;
        }
        return htmlString + "<br>";
    }

    void update(BlockInfo blockInfo) {
        if (mBlockInfo != null && blockInfo.getStartTime().equals(mBlockInfo.getStartTime())) {
            // Same data, nothing to change.
            return;
        }
        mBlockInfo = blockInfo;
        mFoldings = new boolean[getCount()];
        Arrays.fill(mFoldings, true);
        notifyDataSetChanged();
    }

    void toggleRow(int position) {
        mFoldings[position] = !mFoldings[position];
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mBlockInfo == null) {
            return 0;
        }
        return POSITION_FREQUENT_METHOD + 1;
    }

    @Override
    public String getItem(int position) {
        if (getItemViewType(position) == TOP_ROW) {
            return null;
        }
        StringBuilder contentBuilder = new StringBuilder();
        switch (position) {
            case POSITION_TIME:
                return contentBuilder.append("time :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getTimeString()).toString();
            case POSITION_ENV:
                return contentBuilder.append("environment :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getEnvInfo()).toString();
            case POSITION_TOP_HEAVY_METHOD:
                return contentBuilder.append("top-heavy-method :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getTopHeavyMethod()).toString();
            case POSITION_TOP_FREQUENT_METHOD:
                if(TextUtils.isEmpty(mBlockInfo.getTopFrequentMethod())) {
                    return "top-frequent-method : null";
                }
                return contentBuilder.append("top-frequent-method :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getTopFrequentMethod()).toString();
            case POSITION_VIEW:
                if(TextUtils.isEmpty(mBlockInfo.getViewPerformance()))  {
                    if(!ViewPerformanceSampler.isSupported()) {
                        return String.format(Locale.getDefault(), "view-perf : don't support current android version %d", Build.VERSION.SDK_INT);
                    }
                    return "view-perf : null";
                } else {
                    return contentBuilder.append("view-perf :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getViewPerformance()).toString();
                }
            case POSITION_GC:
                if(TextUtils.isEmpty(mBlockInfo.getGcEvent()))  {
                    return "gc-event : null";
                } else {
                    return contentBuilder.append("gc-event :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getGcEvent()).toString();
                }
            case POSITION_HEAVY_METHOD:
                if(TextUtils.isEmpty(mBlockInfo.getHeavyMethods())) {
                    return "heavy-methods : null";
                }
                return contentBuilder.append("heavy-methods :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getHeavyMethods()).toString();
            case POSITION_FREQUENT_METHOD:
            default:
                if(TextUtils.isEmpty(mBlockInfo.getFrequentMethods())) {
                    return "frequent-methods : null";
                }
                return contentBuilder.append("frequent-methods :").append(SEPARATOR).append(SEPARATOR).append(mBlockInfo.getFrequentMethods()).toString();
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TOP_ROW;
        }
        return NORMAL_ROW;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T findById(View view, int id) {
        return (T) view.findViewById(id);
    }
}
