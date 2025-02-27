package sh.siava.AOSPMods.systemui;

import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedBridge.*;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;
import sh.siava.AOSPMods.Utils.FlexStatusIconContainer;
import sh.siava.AOSPMods.Utils.SystemUtils;
import sh.siava.AOSPMods.XPrefs;
import sh.siava.AOSPMods.XposedModPack;

@SuppressWarnings("RedundantThrows")
public class MultiStatusbarRows extends XposedModPack {
    private static final String listenPackage = AOSPMods.SYSTEM_UI_PACKAGE;

    private static boolean systemIconsMultiRow = false;
    private Object ma;

    public MultiStatusbarRows(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        if(Key.length > 0 && Key[0].equals("systemIconsMultiRow"))
        {
            SystemUtils.RestartSystemUI();
        }
        systemIconsMultiRow = XPrefs.Xprefs.getBoolean("systemIconsMultiRow", false);
        FlexStatusIconContainer.setSortPlan(Integer.parseInt(XPrefs.Xprefs.getString("systemIconSortPlan", String.valueOf(FlexStatusIconContainer.SORT_CLEAN))));
    }

    @Override
    public boolean listensTo(String packageName) { return listenPackage.equals(packageName); }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class<?> CollapsedStatusBarFragmentClass = findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);
        Class<?> IconManagerClass = findClass("com.android.systemui.statusbar.phone.StatusBarIconController$IconManager", lpparam.classLoader);

        hookAllConstructors(IconManagerClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if(!systemIconsMultiRow) return;

                try {
                    View linear = (View)param.args[0];

                    mContext.getResources().getResourceName(((View)linear.getParent()).getId()); //helps getting exception if it's in QS

                    FlexStatusIconContainer flex = new FlexStatusIconContainer(mContext, lpparam.classLoader);
                    flex.setPadding(linear.getPaddingLeft(), 0, linear.getPaddingRight(), 0);

                    LinearLayout.LayoutParams flexParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,1);
                    flex.setLayoutParams(flexParams);

                    flex.setForegroundGravity(Gravity.CENTER_VERTICAL | Gravity.END);

                    ViewGroup parent = (ViewGroup)linear.getParent();
                    int index = parent.indexOfChild(linear);
                    parent.addView(flex, index);
                    linear.setVisibility(View.GONE); //remove will crash the system
                    param.args[0] = flex;

                }catch (Throwable ignored){}
            }
        });
    }

}

