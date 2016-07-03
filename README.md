# FuriganaView
FuriganaView is a widget for Android. It renders text simlarily to TextView, but adds furigana on top of Japanese kanji. The furigana has to be supplied to the widget within the text. It also supports bold, italic, bold-italic type and break line with html tag.

## Screenshot

![Example](https://github.com/Rolbackse/FuriganaView/blob/master/Screenshot/Screenshot_20160703-111536.png)

![Example](https://github.com/Rolbackse/FuriganaView/blob/master/Screenshot/Screenshot_20160703-111543.png)

![Example](https://github.com/Rolbackse/FuriganaView/blob/master/Screenshot/Screenshot_20160703-111552.png)

## Implement
FuriganaView has 2 way to implement.

### By java code
```Java
    FuriganaView furiganaView = new FuriganaView(this);
    furiganaView.setJText(YOUR_TEXT);
```

### By xml
```xml
    <com.akira.nguyen.furigana.widget.FuriganaView
        xmlns:furigana="http://schemas.android.com/apk/res/com.akira.nguyen.furigana.sample"
        android:id="@+id/furigana_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="20dp"
        furigana:line_spacing="25"
        furigana:jText="@string/test"/>
```