package com.example.iptvplayer

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private const val PLAYLIST_URL="https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val IA_SEARCH="https://archive.org/advancedsearch.php?q=mediatype%3Amovies%20AND%20licenseurl%3A%28creativecommons.org%29&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description&fl%5B%5D=year&rows=60&page=1&output=json"
private val Bg=Color(0xFF080B10);private val Card=Color(0xFF121820);private val Purple=Color(0xFFA855F7);private val Muted=Color(0xFF8D98A8)
data class Channel(val name:String,val group:String,val url:String)
data class Movie(val id:String,val title:String,val year:String,val description:String)
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{IPTVApp()}}}
@Composable fun IPTVApp(){var page by remember{mutableStateOf("home")};var search by remember{mutableStateOf("")};var channels by remember{mutableStateOf<List<Channel>>(emptyList())};var movies by remember{mutableStateOf<List<Movie>>(emptyList())};var loadingTv by remember{mutableStateOf(true)};var loadingMovies by remember{mutableStateOf(false)};var selected by remember{mutableStateOf<Channel?>(null)};LaunchedEffect(Unit){channels=withContext(Dispatchers.IO){loadM3u(PLAYLIST_URL)};loadingTv=false};LaunchedEffect(page){if(page=="movies"&&movies.isEmpty()&&!loadingMovies){loadingMovies=true;movies=withContext(Dispatchers.IO){loadMoviesFromArchive()};loadingMovies=false}};MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Card,primary=Purple)){if(selected!=null){PlayerScreen(selected!!){selected=null};return@MaterialTheme};Column(Modifier.fillMaxSize().background(Bg)){Text("IPTV Player",Color.White,22.sp,FontWeight.Bold,Modifier.padding(18.dp));Box(Modifier.weight(1f)){when(page){"movies"->MoviePage(movies,search,{search=it},loadingMovies);"channels"->ChannelPage(channels,search,{search=it},loadingTv){selected=it};else->HomePage({page="channels"},{page="movies"})}};BottomBar(page){page=it;search=""}}}}
@Composable private fun HomePage(onTv:()->Unit,onMovies:()->Unit){Column(Modifier.fillMaxSize().padding(16.dp)){Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF6D28D9),Color(0xFF172B8A))),RoundedCornerShape(22.dp)).padding(20.dp)){Column{Text("TV",Color.White.copy(.3f),54.sp,FontWeight.Black);Text("Смотрите любимые каналы",Color.White,17.sp,FontWeight.Bold);Text("и доступное кино",Color.White.copy(.8f),14.sp)}};Spacer(Modifier.height(18.dp));HomeButton("📺","ТВ каналы","Ваш M3U плейлист",onTv);HomeButton("🎬","Фильмы","Internet Archive · открытые лицензии",onMovies)}}
@Composable private fun HomeButton(icon:String,title:String,sub:String,click:()->Unit){Row(Modifier.fillMaxWidth().padding(vertical=5.dp).background(Card,RoundedCornerShape(16.dp)).clickable{click()}.padding(16.dp),Alignment.CenterVertically){Text(icon,30.sp);Column(Modifier.padding(start=14.dp).weight(1f)){Text(title,Color.White,17.sp,FontWeight.Bold);Text(sub,Muted,12.sp)};Text("›",Purple,28.sp)}}
@Composable private fun ChannelPage(ch:List<Channel>,q:String,setQ:(String)->Unit,loading:Boolean,onPlay:(Channel)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){OutlinedTextField(q,setQ,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск канала",color=Muted)});if(loading)Box(Modifier.fillMaxSize(),Alignment.Center){CircularProgressIndicator(color=Purple)}else LazyColumn(contentPadding=PaddingValues(vertical=12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){items(ch.filter{it.name.contains(q,true)||it.group.contains(q,true)}){c->Row(Modifier.fillMaxWidth().background(Card,RoundedCornerShape(14.dp)).clickable{onPlay(c)}.padding(14.dp),Alignment.CenterVertically){Box(Modifier.size(48.dp).background(Color(0xFF273144),RoundedCornerShape(12.dp)),Alignment.Center){Text(c.name.take(2),Color.White,FontWeight.Bold)};Column(Modifier.padding(start=12.dp).weight(1f)){Text(c.name,Color.White,FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(c.group.ifBlank{"ТВ канал"},Muted,12.sp)};Text("▶",Purple)}}}}}
@Composable private fun MoviePage(movies:List<Movie>,q:String,setQ:(String)->Unit,loading:Boolean){val filtered=movies.filter{it.title.contains(q,true)||it.description.contains(q,true)};Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){OutlinedTextField(q,setQ,Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск фильмов",color=Muted)});Spacer(Modifier.height(12.dp));Text("🎬 Internet Archive",Color.White,20.sp,FontWeight.Bold);Text("Фильмы с открытыми лицензиями",Muted,12.sp,Modifier.padding(top=4.dp,bottom=10.dp));if(loading)Box(Modifier.fillMaxSize(),Alignment.Center){CircularProgressIndicator(color=Purple)}else if(filtered.isEmpty())Box(Modifier.fillMaxSize(),Alignment.Center){Text("Ничего не найдено",Muted)}else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)){items(filtered){m->MovieCard(m)}}}}
@Composable private fun MovieCard(m:Movie){val context=LocalContext.current;Row(Modifier.fillMaxWidth().background(Card,RoundedCornerShape(16.dp)).clickable{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://archive.org/details/${m.id}")))}.padding(12.dp),Alignment.CenterVertically){Box(Modifier.width(86.dp).height(116.dp).background(Brush.linearGradient(listOf(Color(0xFF302044),Color(0xFF171D29))),RoundedCornerShape(12.dp)),Alignment.Center){Text("🎬",30.sp)};Column(Modifier.padding(start=14.dp).weight(1f)){Text(m.title,Color.White,16.sp,FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);if(m.year.isNotBlank())Text(m.year,Purple,12.sp);Text(m.description.ifBlank{"Открыть карточку фильма в Internet Archive"},Muted,12.sp,maxLines=3,overflow=TextOverflow.Ellipsis,Modifier.padding(top=5.dp));Text("Открыть →",Purple,13.sp,FontWeight.Bold,Modifier.padding(top=7.dp))}}}
@Composable private fun BottomBar(page:String,go:(String)->Unit){NavigationBar(containerColor=Color(0xFF0D1117)){listOf("home" to "⌂\nГлавная","channels" to "▣\nТВ","movies" to "🎬\nФильмы","settings" to "⚙\nЕще").forEach{(id,label)->NavigationBarItem(page==id,{go(id)},{Text(label,fontSize=10.sp)},label=null)}}}
@Composable private fun PlayerScreen(c:Channel,back:()->Unit){val ctx=LocalContext.current;val p=remember(c.url){ExoPlayer.Builder(ctx).build().apply{setMediaItem(MediaItem.fromUri(c.url));prepare();playWhenReady=true}};DisposableEffect(p){onDispose{p.release()}};Column(Modifier.fillMaxSize().background(Color.Black)){Text("‹  ${c.name}",Color.White,18.sp,Modifier.clickable{back()}.padding(14.dp));AndroidView(factory={PlayerView(it).apply{player=p}},Modifier.fillMaxWidth().aspectRatio(16f/9f));Text("● LIVE  ${c.group}",Color(0xFFFF4B4B),Modifier.padding(16.dp))}}
private fun loadM3u(url:String)=try{parseM3u(URL(url).readText())}catch(_:Exception){emptyList()}
private fun parseM3u(t:String):List<Channel>{val r=mutableListOf<Channel>();var name="";var group="";t.lineSequence().forEach{l->val x=l.trim();when{ x.startsWith("#EXTINF:",true)->{name=x.substringAfterLast(",").trim();group=Regex("""group-title=\"([^\"]*)\"""",RegexOption.IGNORE_CASE).find(x)?.groupValues?.getOrNull(1).orEmpty()};x.startsWith("#")||x.isBlank()->{};else->{if(name.isNotBlank())r+=Channel(name,group,x);name="";group=""}}};return r}
private fun loadMoviesFromArchive():List<Movie>{return try{val root=JSONObject(URL(IA_SEARCH).readText());val docs=root.getJSONObject("response").getJSONArray("docs");buildList{for(i in 0 until docs.length()){val d=docs.getJSONObject(i);val id=d.optString("identifier");val title=d.optString("title");if(id.isNotBlank()&&title.isNotBlank())add(Movie(id,title,d.optString("year"),d.optString("description")))}}}catch(_:Exception){emptyList()}}
