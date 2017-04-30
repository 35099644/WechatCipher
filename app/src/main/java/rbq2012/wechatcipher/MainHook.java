package rbq2012.wechatcipher;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import android.content.Context;
import de.robv.android.xposed.XposedBridge;
import android.util.AttributeSet;
import android.widget.Toast;
import android.widget.TextView;

public class MainHook extends XC_MethodHook
implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources{

	static private String LOG_FILE="/sdcard/#xp/log.txt";
	static private String PM_MM="com.tencent.mm";
	
	static public EditText inpu=null;
	
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p1) throws Throwable{
		if(!p1.packageName.equals(PM_MM)) return;
		flog("handleLoadPackage");
		/* 在为发送按钮设置OnClickListener时，将微信原设置的Listener
		  * 备份，然后将传入参数替换成自定义的Listener，以便在点击按
		  * 钮前执行自定义的代码，再执行备份的原Listener的点击事件，
		  * 使微信将消息发送出去。
		  */
		XC_MethodHook hooksend=new XC_MethodHook(){
			@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
				View btn=(View)param.thisObject;
				if(btn.getId()!=2131756121) return;
				btn.setTag(btn.getId());
				btn.setTag(param.args[0]);
				param.args[0]=new OnClickListener(){
						@Override
						public void onClick(View p1){
							//插入在触发点击事件之前进行的处理，比如加密输入框的文本
							inpu.setText(Cryptoo.encryptAES(inpu.getText().toString(),"1234"));
							OnClickListener fuck=(View.OnClickListener) (p1.getTag());
							fuck.onClick(p1);
							//插入在触发点击事件之后进行的处理，比如显示提示信息
						}
					};
			}
			@Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable{
			}
		};
		findAndHookMethod(View.class,"setOnClickListener",View.OnClickListener.class,hooksend);
		////
		
		/* 通过构造函数找到消息输入框，并保存。
		  */
		XC_MethodHook hooket=new XC_MethodHook(){
			@Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable{
				View v=(View) param.thisObject;
				if(v.getId()==2131756115){
					inpu=(EditText)v;
				}
			}
		};
		findAndHookConstructor(EditText.class,Context.class,AttributeSet.class,hooket);
		////
		
		/* 文字气泡被微信设置文字时，先将文字解密
		  */
		XC_MethodHook hookbub=new XC_MethodHook(){
			@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable{
				View v=(View)param.thisObject;
				if(v.getId()!=2131755346) return;
				param.args[0]=Cryptoo.decryptAES((String)(param.args[0]),"1234");
			}
		};
		findAndHookMethod(TextView.class,"setText",CharSequence.class,hookbub);
	}

	@Override
	public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam p1) throws Throwable{
		if(!p1.packageName.equals(PM_MM)) return;
		flog("handleInitPackageResources");
	}

	@Override
	public void initZygote(IXposedHookZygoteInit.StartupParam p1) throws Throwable{
		flog("initZygote");
	}

	static private void flog(String s){
		try{
		Logger.setupIfNeed(new File(LOG_FILE));
		Logger.log(s);
		}catch(Exception e){}
	}
	
}
