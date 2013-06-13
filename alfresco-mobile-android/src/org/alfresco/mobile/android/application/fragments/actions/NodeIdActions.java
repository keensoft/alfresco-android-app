/*******************************************************************************
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 * 
 * This file is part of Alfresco Mobile for Android.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.alfresco.mobile.android.application.fragments.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.mobile.android.api.model.Folder;
import org.alfresco.mobile.android.api.model.Node;
import org.alfresco.mobile.android.application.R;
import org.alfresco.mobile.android.application.fragments.browser.ChildrenBrowserFragment;
import org.alfresco.mobile.android.application.fragments.favorites.FavoritesSyncFragment;
import org.alfresco.mobile.android.application.fragments.menu.MenuActionItem;
import org.alfresco.mobile.android.application.fragments.operations.OperationWaitingDialogFragment;
import org.alfresco.mobile.android.application.fragments.properties.DetailsFragment;
import org.alfresco.mobile.android.application.intent.IntentIntegrator;
import org.alfresco.mobile.android.application.manager.StorageManager;
import org.alfresco.mobile.android.application.operations.OperationRequest;
import org.alfresco.mobile.android.application.operations.OperationsRequestGroup;
import org.alfresco.mobile.android.application.operations.batch.BatchOperationManager;
import org.alfresco.mobile.android.application.operations.batch.node.delete.DeleteNodeRequest;
import org.alfresco.mobile.android.application.operations.batch.node.favorite.FavoriteNodeRequest;
import org.alfresco.mobile.android.application.operations.batch.node.like.LikeNodeRequest;
import org.alfresco.mobile.android.application.utils.SessionUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

@TargetApi(11)
public class NodeIdActions implements ActionMode.Callback
{
    public static final String TAG = "NodeActions";

    private List<String> selectedNodeIds = new ArrayList<String>();

    private onFinishModeListerner mListener;

    private ActionMode mode;

    private Activity activity = null;

    private Fragment fragment;

    public NodeIdActions(Fragment f, List<String> selectedNodes)
    {
        this.fragment = f;
        this.activity = f.getActivity();
        this.selectedNodeIds = selectedNodes;
        for (String nodeId : selectedNodes)
        {
            addNode(nodeId);
        }
    }

    // ///////////////////////////////////////////////////////////////////////////
    // LIFECYCLE
    // ///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu)
    {
        this.mode = mode;
        getMenu(menu);
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu)
    {
        mode.setTitle(createTitle());
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode)
    {
        mListener.onFinish();
        selectedNodeIds.clear();
    }

    public void finish()
    {
        mode.finish();
    }

    // ///////////////////////////////////////////////////////////////////////////
    // INTERNALS
    // ///////////////////////////////////////////////////////////////////////////
    private String createTitle()
    {
        String title = "";

        int size = selectedNodeIds.size();
        if (size > 0)
        {
            title += String.format(activity.getResources().getQuantityString(R.plurals.selected_document, size), size);
        }

        return title;
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // LIST MANAGEMENT
    // ///////////////////////////////////////////////////////////////////////////////////
    public void selectNode(String n)
    {
        if (selectedNodeIds.contains(n))
        {
            removeNode(n);
        }
        else
        {
            addNode(n);
        }
        if (selectedNodeIds.isEmpty())
        {
            mode.finish();
        }
        else
        {
            mode.setTitle(createTitle());
            mode.invalidate();
        }
    }

    public void selectNodes(List<String> nodes)
    {
        selectedNodeIds.clear();
        for (String node : nodes)
        {
            addNode(node);
        }
        mode.setTitle(createTitle());
        mode.invalidate();
    }

    private void addNode(String n)
    {
        if (n == null) { return; }

        if (!selectedNodeIds.contains(n))
        {
            selectedNodeIds.add(n);
        }
    }

    private void removeNode(String n)
    {
        selectedNodeIds.remove(n);
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // MENU
    // ///////////////////////////////////////////////////////////////////////////////////
    private void getMenu(Menu menu)
    {
        menu.clear();
        getMenu(activity, menu);
    }

    private void getMenu(Activity activity, Menu menu)
    {
        MenuItem mi;
        SubMenu createMenu;

        createMenu = menu.addSubMenu(Menu.NONE, MenuActionItem.MENU_FAVORITE_GROUP, Menu.FIRST
                + MenuActionItem.MENU_FAVORITE_GROUP, R.string.favorite);
        createMenu.setIcon(R.drawable.ic_favorite_dark);
        createMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        createMenu.add(Menu.NONE, MenuActionItem.MENU_FAVORITE_GROUP_FAVORITE, Menu.FIRST
                + MenuActionItem.MENU_FAVORITE_GROUP_FAVORITE, R.string.favorite);
        createMenu.add(Menu.NONE, MenuActionItem.MENU_FAVORITE_GROUP_UNFAVORITE, Menu.FIRST
                + MenuActionItem.MENU_FAVORITE_GROUP_UNFAVORITE, R.string.unfavorite);
        
        createMenu = menu.addSubMenu(Menu.NONE, MenuActionItem.MENU_LIKE_GROUP, Menu.FIRST
                + MenuActionItem.MENU_LIKE_GROUP, R.string.like);
        createMenu.setIcon(R.drawable.ic_like);
        createMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        createMenu.add(Menu.NONE, MenuActionItem.MENU_LIKE_GROUP_LIKE,
                Menu.FIRST + MenuActionItem.MENU_LIKE_GROUP_LIKE, R.string.like);
        createMenu.add(Menu.NONE, MenuActionItem.MENU_LIKE_GROUP_UNLIKE, Menu.FIRST
                + MenuActionItem.MENU_LIKE_GROUP_UNLIKE, R.string.unlike);

        mi = menu.add(Menu.NONE, MenuActionItem.MENU_DELETE, Menu.FIRST + MenuActionItem.MENU_DELETE, R.string.delete);
        mi.setIcon(R.drawable.ic_delete);
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        mi = menu.add(Menu.NONE, MenuActionItem.MENU_OPERATIONS, Menu.FIRST + MenuActionItem.MENU_OPERATIONS,
                R.string.operations);
        mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item)
    {
        Boolean b = false;
        switch (item.getItemId())
        {
            case MenuActionItem.MENU_FAVORITE_GROUP_FAVORITE:
                favorite(true);
                b = true;
                break;
            case MenuActionItem.MENU_FAVORITE_GROUP_UNFAVORITE:
                favorite(false);
                b = true;
                break;
            case MenuActionItem.MENU_LIKE_GROUP_LIKE:
                like(true);
                b = true;
                break;
            case MenuActionItem.MENU_LIKE_GROUP_UNLIKE:
                like(false);
                b = true;
                break;
            case MenuActionItem.MENU_OPERATIONS:
                activity.startActivity(new Intent(IntentIntegrator.ACTION_DISPLAY_OPERATIONS));
                b = false;
                break;
            default:
                break;
        }
        if (b)
        {
            selectedNodeIds.clear();
            mode.finish();
        }
        return b;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // ACTIONS
    // ///////////////////////////////////////////////////////////////////////////
    private void favorite(boolean doFavorite)
    {
        OperationsRequestGroup group = new OperationsRequestGroup(activity, SessionUtils.getAccount(activity));
        for (String node : selectedNodeIds)
        {
            group.enqueue(new FavoriteNodeRequest(null, node, doFavorite)
                    .setNotificationVisibility(OperationRequest.VISIBILITY_DIALOG));
        }

        BatchOperationManager.getInstance(activity).enqueue(group);

        if (fragment instanceof ChildrenBrowserFragment || fragment instanceof FavoritesSyncFragment)
        {
            int titleId = R.string.unfavorite;
            int iconId = R.drawable.ic_unfavorite_dark;
            if (doFavorite)
            {
                titleId = R.string.favorite;
                iconId = R.drawable.ic_favorite_dark;
            }
            OperationWaitingDialogFragment.newInstance(FavoriteNodeRequest.TYPE_ID, iconId,
                    fragment.getString(titleId), null, null, selectedNodeIds.size()).show(
                    fragment.getActivity().getFragmentManager(), OperationWaitingDialogFragment.TAG);
        }
    }

    private void like(boolean doLike)
    {
        OperationsRequestGroup group = new OperationsRequestGroup(activity, SessionUtils.getAccount(activity));
        for (String node : selectedNodeIds)
        {
            group.enqueue(new LikeNodeRequest(null, node, doLike)
                    .setNotificationVisibility(OperationRequest.VISIBILITY_DIALOG));
        }
        BatchOperationManager.getInstance(activity).enqueue(group);

        if (fragment instanceof ChildrenBrowserFragment)
        {
            int titleId = R.string.unlike;
            int iconId = R.drawable.ic_unlike;
            if (doLike)
            {
                titleId = R.string.like;
                iconId = R.drawable.ic_like;
            }
            OperationWaitingDialogFragment.newInstance(LikeNodeRequest.TYPE_ID, iconId, fragment.getString(titleId),
                    null, null, selectedNodeIds.size()).show(fragment.getActivity().getFragmentManager(),
                    OperationWaitingDialogFragment.TAG);
        }
    }

    public static void delete(final Activity activity, final Fragment f, Node node)
    {
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(node);
        delete(activity, f, nodes);
    }

    public static void delete(final Activity activity, final Fragment f, final List<Node> nodes)
    {
        Folder tmpParent = null;
        if (f instanceof ChildrenBrowserFragment)
        {
            tmpParent = ((ChildrenBrowserFragment) f).getParent();
        }
        else if (f instanceof DetailsFragment)
        {
            tmpParent = ((DetailsFragment) f).getParentNode();
        }
        final Folder parent = tmpParent;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.delete);
        String nodeDescription = nodes.size() + "";
        if (nodes.size() == 1)
        {
            nodeDescription = nodes.get(0).getName();
        }
        String description = String.format(
                activity.getResources().getQuantityString(R.plurals.delete_items, nodes.size()), nodeDescription);
        builder.setMessage(description);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int item)
            {
                OperationsRequestGroup group = new OperationsRequestGroup(activity, SessionUtils.getAccount(activity));

                if (nodes.size() == 1)
                {
                    group.enqueue(new DeleteNodeRequest(parent, nodes.get(0))
                            .setNotificationVisibility(OperationRequest.VISIBILITY_TOAST));
                }
                else
                {
                    for (Node node : nodes)
                    {
                        group.enqueue(new DeleteNodeRequest(parent, node)
                                .setNotificationVisibility(OperationRequest.VISIBILITY_DIALOG));
                    }

                    if (f instanceof ChildrenBrowserFragment)
                    {
                        OperationWaitingDialogFragment.newInstance(DeleteNodeRequest.TYPE_ID, R.drawable.ic_delete,
                                f.getString(R.string.delete), null, parent, nodes.size()).show(
                                f.getActivity().getFragmentManager(), OperationWaitingDialogFragment.TAG);
                    }
                }

                BatchOperationManager.getInstance(activity).enqueue(group);

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int item)
            {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static File getDownloadFile(final Activity activity, final Node node)
    {
        if (activity != null && node != null && SessionUtils.getAccount(activity) != null)
        {
            File folder = StorageManager.getDownloadFolder(activity, SessionUtils.getAccount(activity));
            if (folder != null) { return new File(folder, node.getName()); }
        }

        return null;
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // LISTENER
    // ///////////////////////////////////////////////////////////////////////////////////
    public interface onFinishModeListerner
    {
        void onFinish();
    }

    public void setOnFinishModeListerner(onFinishModeListerner mListener)
    {
        this.mListener = mListener;
    }
}