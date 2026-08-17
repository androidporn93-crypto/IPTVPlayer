package com.example.iptvplayer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val IA_SEARCH = "https://archive.org/advancedsearch.php?q=mediatype%3Amovies%20AND%20%28licenseurl%3Acreativecommons.org%20OR%20licenseurl%3Apublicdomain%29&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description&fl%5B%5D=year&fl%5B%5D=licenseurl&rows=60&page=1&output=json"
private const val UA = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"
private val BG = Color(0xFF080B10)
private val CARD = Color(0xFF121820)
private val PURPLE = Color(0xFFA855F7)
private val MUTED = Color(0xFF8D98A8)

data class Channel(val name:String,val group:String,val url:String,val userAgent:String="")
data class Movie(val id:String,val title:String,val year:String,val description:String,val license:String)

class MainActivity: ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);setContent{IPTVApp()}}
}

@Composable
fun IPTVApp(){
    var page by remember{mutableStateOf("home")}
    var query by remember{mutableStateOf("")}
    var channels by remember{mutableStateOf<List<Channel>>(emptyList())}
    var movies by remember{mutableStateOf<List<Movie>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var movieLoading by remember{mutableStateOf(false)}
    var selected by remember{mutableStateOf<Int?>(null)}
    LaunchedEffect(Unit){channels=withContext(Dispatchers.IO){loadM3u()};loading=false}
    LaunchedEffect(page){if(page=="movies"&&movies.isEmpty()&&!movieLoading){movieLoading=true;movies=withContext(Dispatchers.IO){loadMovies()};movieLoading=false}}
    MaterialTheme(colorScheme=darkColorScheme(background=BG,surface=CARD,primary=PURPLE)){
        if(selected!=null&&channels.isNotEmpty()){PlayerScreen(channels,selected!!,{selected=it}){selected=null};return@MaterialTheme}
        Scaffold(containerColor=BG,bottomBar={BottomBar(page){page=it;query=""}}){p->
            Column(Modifier.fillMaxSize().background(BG).padding(p)){
                Text("IPTV Player",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(18.dp))
                Box(Modifier.weight(1f)){when(page){"channels"->Channels(channels,query,{query=it},loading){selected=it};"movies"->Movies(movies,query,{query=it},movieLoading);else->Home({page="channels"},{page="movies"})}}
            }
        }
    }
}

@Composable private fun Home(tv:()->Unit,movie:()->Unit){Column(Modifier.fillMaxSize().padding(16.dp)){Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF6D28D9),Color(0xFF172B8A))),RoundedCornerShape(22.dp)).padding(20.dp)){Column{Text("TV",color=Color.White.copy(.28f),fontSize=54.sp,fontWeight=FontWeight.Black);Text("Смотрите любимые каналы",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold);Text("и доступное кино",color=Color.White.copy(.8f))}};Spacer(Modifier.height(18.dp));HomeBtn("📺","ТВ каналы","Ваш M3U плейлист",tv);HomeBtn("🎬","Фильмы","Internet Archive · открытые лицензии",movie)}}
@Composable private fun HomeBtn(icon:String,title:String,sub:String,on:()->Unit){Row(Modifier.fillMaxWidth().padding(vertical=5.dp).background(CARD,RoundedCornerShape(16.dp)).clickable(onClick=on).padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(icon,fontSize=30.sp);Column(Modifier.padding(start=14.dp).weight(1f)){Text(title,color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold);Text(sub,color=MUTED,fontSize=12.sp)};Text("›",color=PURPLE,fontSize=28.sp)}}

@Composable private fun Channels(channels:List<Channel>,q:String,onQ:(String)->Unit,loading:Boolean,onPlay:(Int)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){OutlinedTextField(q,onQ,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск канала",color=MUTED)});if(loading)Box(Modifier.fillMaxSize(),Alignment.Center){CircularProgressIndicator(color=PURPLE)}else{val list=channels.withIndex().filter{it.value.name.contains(q,true)||it.value.group.contains(q,true)};LazyColumn(contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(list){_,item->ChannelRow(item.index,item.value,onPlay)}}}}}
@Composable private fun ChannelRow(index:Int,c:Channel,on:(Int)->Unit){Row(Modifier.fillMaxWidth().background(CARD,RoundedCornerShape(14.dp)).clickable{on(index)}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).background(Color(0xFF273144),RoundedCornerShape(12.dp)),Alignment.Center){Text("${index+1}",color=Color.White,fontWeight=FontWeight.Bold)};Column(Modifier.padding(start=12.dp).weight(1f)){Text(c.name,color=Color.White,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(c.group.ifBlank{"ТВ канал"},color=MUTED,fontSize=12.sp)};Text("▶",color=PURPLE)}}

@Composable private fun PlayerScreen(channels:List<Channel>,index:Int,onSelect:(Int)->Unit,onBack:()->Unit){
    val context=LocalContext.current;val activity=context as ComponentActivity;val c=channels[index]
    var full by remember{mutableStateOf(false)};var settings by remember{mutableStateOf(false)};var err by remember{mutableStateOf<String?>(null)}
    val player=remember(c.url,c.userAgent){
        val http=DefaultHttpDataSource.Factory().setUserAgent(c.userAgent.ifBlank{UA}).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(15000)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http)).build().apply{
            playWhenReady=true
            addListener(object:Player.Listener{override fun onPlayerError(error:PlaybackException){err="Не удалось загрузить канал\n${error.errorCodeName}"};override fun onPlaybackStateChanged(state:Int){if(state==Player.STATE_READY)err=null}})
            setMediaItem(MediaItem.fromUri(c.url));prepare()
        }
    }
    DisposableEffect(player){onDispose{player.release();activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}}
    BackHandler{if(settings)settings=false else if(full)full=false else onBack()}
    LaunchedEffect(full){if(full){activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;WindowCompat.setDecorFitsSystemWindows(activity.window,false);WindowInsetsControllerCompat(activity.window,activity.window.decorView).apply{hide(WindowInsetsCompat.Type.systemBars());systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}}else{activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;WindowCompat.setDecorFitsSystemWindows(activity.window,true);WindowInsetsControllerCompat(activity.window,activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())}}
    val pv: @Composable (Boolean)->Unit={zoom->AndroidView(factory={ctx->PlayerView(ctx).apply{this.player=player;keepScreenOn=true;resizeMode=if(zoom)AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT;controllerShowTimeoutMs=2500;controllerHideOnTouch=true;post{hideControls(this)}}},update={hideControls(it)},modifier=Modifier.fillMaxSize())}
    if(full){Box(Modifier.fillMaxSize().background(Color.Black)){pv(true);Overlay(settings={settings=true},full={full=false},Modifier.align(Alignment.BottomEnd));err?.let{Text(it,color=Color.White,modifier=Modifier.align(Alignment.Center).padding(24.dp))}}}
    else{Column(Modifier.fillMaxSize().background(BG)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Text("‹",color=Color.White,fontSize=32.sp,modifier=Modifier.clickable{onBack()});Text(c.name,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(start=10.dp).weight(1f))};Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black)){pv(false);Overlay({settings=true},{full=true},Modifier.align(Alignment.BottomEnd));err?.let{Text(it,color=Color.White,modifier=Modifier.align(Alignment.Center).padding(24.dp))}};Text("${index+1}. ${c.name}",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(16.dp,14.dp,16.dp,6.dp));Text("Следующие каналы",color=MUTED,fontSize=13.sp,modifier=Modifier.padding(horizontal=16.dp));LazyColumn(contentPadding=PaddingValues(12.dp,10.dp,12.dp,24.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(channels.drop(index+1)){off,next->val i=index+1+off;Row(Modifier.fillMaxWidth().background(CARD,RoundedCornerShape(13.dp)).clickable{onSelect(i)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text("${i+1}",color=PURPLE,fontWeight=FontWeight.Bold,modifier=Modifier.width(36.dp));Column(Modifier.weight(1f)){Text(next.name,color=Color.White,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(next.group.ifBlank{"ТВ канал"},color=MUTED,fontSize=11.sp)};Text("▶",color=PURPLE)}}}}}
    if(settings)AlertDialog(onDismissRequest={settings=false},title={Text("Настройки плеера")},text={Text("Настройки подключения и воспроизведения\n\nUser-Agent берётся из M3U автоматически.",color=MUTED)},confirmButton={TextButton({settings=false}){Text("Закрыть")}})
}

@Composable private fun Overlay(settings:()->Unit,full:()->Unit,modifier:Modifier){Row(modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.size(44.dp).background(Color.Black.copy(.55f),RoundedCornerShape(22.dp)).clickable{settings()},Alignment.Center){Text("⚙",color=Color.White,fontSize=22.sp)};Box(Modifier.size(44.dp).background(Color.Black.copy(.55f),RoundedCornerShape(22.dp)).clickable{full()},Alignment.Center){Text("⛶",color=Color.White,fontSize=23.sp)}}}
private fun hideControls(v:PlayerView){listOf(androidx.media3.ui.R.id.exo_settings,androidx.media3.ui.R.id.exo_rew,androidx.media3.ui.R.id.exo_ffwd,androidx.media3.ui.R.id.exo_prev,androidx.media3.ui.R.id.exo_next).forEach{id->v.findViewById<View>(id)?.visibility=View.GONE}}

@Composable private fun Movies(movies:List<Movie>,q:String,onQ:(String)->Unit,loading:Boolean){val f=movies.filter{it.title.contains(q,true)||it.description.contains(q,true)};Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){OutlinedTextField(q,onQ,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск фильмов",color=MUTED)});Spacer(Modifier.height(12.dp));Text("🎬 Internet Archive",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Public Domain / Creative Commons",color=MUTED,fontSize=12.sp);when{loading->Box(Modifier.fillMaxSize(),Alignment.Center){CircularProgressIndicator(color=PURPLE)};f.isEmpty()->Box(Modifier.fillMaxSize(),Alignment.Center){Text("Каталог пуст или источник недоступен",color=MUTED)};else->LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){itemsIndexed(f){_,m->MovieCard(m)}}}}}
@Composable private fun MovieCard(m:Movie){val ctx=LocalContext.current;Row(Modifier.fillMaxWidth().background(CARD,RoundedCornerShape(16.dp)).clickable{ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://archive.org/details/${m.id}")))}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model="https://archive.org/services/img/${m.id}",contentDescription=m.title,modifier=Modifier.width(88.dp).height(118.dp).background(Color(0xFF222A35),RoundedCornerShape(12.dp)));Column(Modifier.padding(start=14.dp).weight(1f)){Text(m.title,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);if(m.year.isNotBlank())Text(m.year,color=PURPLE,fontSize=12.sp);Text(m.description.ifBlank{"Открыть карточку фильма в Internet Archive"},color=MUTED,fontSize=12.sp,maxLines=3,overflow=TextOverflow.Ellipsis);Text("▶ Открыть и смотреть",color=PURPLE,fontSize=13.sp,fontWeight=FontWeight.Bold)}}}
@Composable private fun BottomBar(page:String,on:(String)->Unit){NavigationBar(containerColor=Color(0xFF0D1117)){listOf(Triple("home","⌂","Главная"),Triple("channels","▣","ТВ"),Triple("movies","🎬","Фильмы"),Triple("settings","⚙","Ещё")).forEach{(id,icon,label)->NavigationBarItem(page==id,{on(id)},{Text(icon,fontSize=20.sp)},{Text(label,fontSize=10.sp)})}}}

private fun loadM3u()=try{parseM3u(URL(PLAYLIST_URL).readText())}catch(_:Exception){emptyList<Channel>()}
private fun parseM3u(text:String):List<Channel>{val out=mutableListOf<Channel>();var name="";var group="";var ua="";text.lineSequence().forEach{l0->val l=l0.trim();when{l.startsWith("#EXTINF:",true)->{name=l.substringAfterLast(',').trim();group=Regex("""group-title=\"([^\"]*)\"""",RegexOption.IGNORE_CASE).find(l)?.groupValues?.getOrNull(1).orEmpty();ua=""};l.startsWith("#EXTVLCOPT:http-user-agent=",true)->ua=l.substringAfter("=","").trim();l.startsWith("#")||l.isBlank()->Unit;else->{if(name.isNotBlank())out+=Channel(name,group,l,ua);name="";group="";ua=""}}};return out}
private fun loadMovies()=try{val d=JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs");buildList{for(i in 0 until d.length()){val x=d.getJSONObject(i);val id=x.optString("identifier");val t=x.optString("title");val l=x.optString("licenseurl");if(id.isNotBlank()&&t.isNotBlank()&&(l.contains("creativecommons.org",true)||l.contains("publicdomain",true)))add(Movie(id,t,x.optString("year"),x.optString("description"),l))}}.distinctBy{it.id}}catch(_:Exception){emptyList()}
