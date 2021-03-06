package com.akira.nguyen.furigana.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.akira.nguyen.furigana.widget.FuriganaView;

/**
 * Created by Akira on 2016/07/03.
 */
public class MainActivity extends AppCompatActivity  implements FuriganaView.OnTextSelectedListener{
    private static final String TEST = "<b>{日本語;にほんご}が{簡単;かんたん}だよ。</b><br/><i>" +
            "{日本語;にほんご}は、{主;しゅ}に{日本;にほん}{国内;こくない}や{日本人;にほんじん}{同士;どうし}" +
            "の{間;あいだ}で{使;つか}われている{言語;げんご}である。</i>{日本;にほん}は{法令;ほうれい}によって" +
            "「{公用語;こうようご}」を{規定;きてい}していないが、{法令;ほうれい}その{他;た}の{公用文;こうようぶん}" +
            "は{全て;すべて}{日本語;にほんご}で{記述;きじゅつ}され、{各種;かくしゅ}{法令;ほうれい}において{日本語;にほんご}" +
            "を{用いる;もちいる}ことが{定め;さだめ}られるなど{事実上;じじつじょう}の{公用語;こうようご}となっており、" +
            "{学校;がっこう}{教育;きょういく}の「{国語;こくご}」でも{教え;おしえ}られる。<br /><b>{使用;しよう}{人口;じんこう}" +
            "について{正確;せいかく}な{統計;とうけい}はないが、{日本;にほん}{国内;こくない}の{人口;じんこう}、および{日本;にほん}" +
            "{国外;こくがい}に{住む;すむ}{日本人;にほんじん}や{日系人;にっけいじん}、{日本;にほん}がかつて{統治;とうち}した" +
            "{地域;ちいき}の{一部;いちぶ}{じゅうみん;}など、{約;やく}1{億;おく}3{千万;せんまん}{人;ひと}{以上;いじょう}と" +
            "{考え;かんがえ}られている。<i>{統計;とうけい}によって{前後;ぜんご}する{可能性;かのうせい}はあるが、この{数;かず}" +
            "は{世界;せいかい}の{母語話者;ぼごわしゃ}{数;すう}で{上位;じょうい}10{位;い}{以内;いない}に{入る;はいる}{人数;にんずう}" +
            "である。<br></i></b><b><i>{日本;にほん}で{生まれ;うまれ}{育った;そだった}ほとんどの{人;ひと}は、{日本語;にほんご}" +
            "を{母語;ぼご}とする。</i>{日本語;にほんご}の{文法;ぶんぽう}{体系;たいけい}や{音韻;おんいん}{体系;たいけい}を{反映;はんえい}" +
            "する{手話;しゅわ}として{日本語;にほんご}{対応;たいおう}{手話;しゅわ}がある。\n" +
            "</b><i>2013{年;ねん}1{月;がつ}{現在;げんざい}、インターネット{上;じょう}の" +
            "{言語;げんご}{使用者;しようしゃ}{数;すう}は、{英語;えいご}、{中国語;ちゅうごくご}、スペイン{語;ご}、アラビア{語;ご}、" +
            "ポルトガル{語;ご}に{次いで;ついで}6{番目;ばんめ}に{多い;おおい}。</i>";
    private FuriganaView mFuriganaView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFuriganaView = (FuriganaView) findViewById(R.id.furigana_view);
        mFuriganaView.setOnTextSelectedListener(this);

        findViewById(R.id.set_text_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFuriganaView.setJText(TEST);
            }
        });

        findViewById(R.id.reset_text_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFuriganaView.resetText();
            }
        });
    }

    @Override
    public void onTextSelected(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
