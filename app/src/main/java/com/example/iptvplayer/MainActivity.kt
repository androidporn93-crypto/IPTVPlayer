package com.example.iptvplayer

import android.os.Bundle
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
import androidx.compose.ui.draw.clip
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
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private val Bg = Color(0xFF080B10)
private val Card = Color(0xFF121820)
private val Card2 = Color(0xFF171E28)
private val Purple = Color(0xFFA855F7)
private val Purple2 = Color(0xFF6D28D9)
private val Muted = Color(0xFF8D98A8)

data class Channel(val name:String,val group:String,val logo:String?,val url:String,val userAgent:String?=null)
data class Movie(val title:String,val year:String,val description:String)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{IPTVApp()}}
}

@Composable
fun IPTVApp(){
    var channels by remember{mutableStateOf<List<Channel>>(emptyList())}
    var selected by remember{mutableStateOf<Channel?>(null)}
    var page by remember{mutableStateOf("home")}
    var search by remember{mutableStateOf("")}
    var loading by remember{mutableStateOf(true)}
    var drawer by remember{mutableStateOf(false)}
    var favorites by remember{mutableStateOf(setOf<String>())}
    val movies=remember{legalMovies()}

    LaunchedEffect(Unit){channels=withContext(Dispatchers.IO){loadM3u(PLAYLIST_URL)};loading=false}

    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Card,primary=Purple)){
        if(selected!=null){PlayerScreen(selected!!){selected=null};return@MaterialTheme}
        Box(Modifier.fillMaxSize().background(Bg)){
            Column(Modifier.fillMaxSize()){
                TopBar(when(page){"channels"->"ТВ каналы";"movies"->"Фильмы";"favorites"->"Избранное";"settings"->"Настройки";else->"IPTV Player"},{drawer=true})
                if(loading&&page!="movies")Box(Modifier.fillMaxSize(),Alignment.Center){CircularProgressIndicator(color=Purple)}
                else when(page){
                    "channels"->ChannelPage(channels,search,{search=it},favorites,{favorites=toggle(favorites,it.name)},{selected=it})
                    "movies"->MoviePage(movies,search){search=it}
                    "favorites"->ChannelPage(channels.filter{it.name in favorites},search,{search=it},favorites,{favorites=toggle(favorites,it.name)},{selected=it})
                    "settings"->SettingsPage()
                    else->HomePage(channels,favorites,{selected=it},{page="channels"},{page="movies"})
                }
                if(page!="settings")BottomBar(page){page=it;search=""}
            }
            if(drawer)Drawer({drawer=false}){page=it;search="";drawer=false}
        }
    }
}

private fun toggle(s:Set<String>,name:String)=if(name in s)s-name else s+name

@Composable private fun TopBar(title:String,onMenu:()->Unit){
    Row(Modifier.fillMaxWidth().padding(16.dp,14.dp),Alignment.CenterVertically){
        Text("☰",Color.White,24.sp,modifier=Modifier.clickable{onMenu()});Spacer(Modifier.width(18.dp))
        Text(title,Color.White,20.sp,FontWeight.Bold,modifier=Modifier.weight(1f));Text("IPTV",Purple,12.sp,FontWeight.Bold)
    }
}

@Composable private fun HomePage(channels:List<Channel>,favorites:Set<String>,onPlay:(Channel)->Unit,onAll:()->Unit,onMovies:()->Unit){
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Purple2,Color(0xFF172B8A))))){
            Column(Modifier.padding(20.dp).align(Alignment.CenterStart)){Text("TV",Color.White.copy(.25f),58.sp,FontWeight.Black);Text("Смотрите любимые каналы",Color.White,17.sp,FontWeight.Bold);Text("в отличном качестве",Color.White.copy(.85f),14.sp)}
        }
        Spacer(Modifier.height(16.dp));Row(Alignment.CenterVertically){Text("Сейчас в эфире",Color.White,17.sp,FontWeight.Bold,modifier=Modifier.weight(1f));Text("Смотреть все",Purple,13.sp,modifier=Modifier.clickable{onAll()})}
        Spacer(Modifier.height(8.dp));LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=10.dp)){
            items(channels.take(5)){c->ChannelRow(c,c.name in favorites,{onPlay(c)},{})}
            item{Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF28163D),Color(0xFF151D2B)))).clickable{onMovies()}.padding(16.dp),Alignment.CenterVertically){Text("🎬",fontSize=34.sp);Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text("Фильмы",Color.White,17.sp,FontWeight.Bold);Text("Только доступный легальный контент",Muted,12.sp)};Text("›",Purple,28.sp)}}
        }
    }
}

@Composable private fun ChannelPage(channels:List<Channel>,search:String,onSearch:(String)->Unit,favorites:Set<String>,onFavorite:(Channel)->Unit,onPlay:(Channel)->Unit){
    val filtered=channels.filter{it.name.contains(search,true)||it.group.contains(search,true)}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        OutlinedTextField(search,onSearch,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск канала",color=Muted)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Card2,focusedTextColor=Color.White,unfocusedTextColor=Color.White,cursorColor=Purple))
        Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Все","Новости","Спорт","Кино").forEachIndexed{i,x->FilterChip(i==0,{},label={Text(x)})}}
        Text("${filtered.size} каналов",Muted,13.sp,modifier=Modifier.padding(vertical=8.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp),contentPadding=PaddingValues(bottom=80.dp)){items(filtered){c->ChannelRow(c,c.name in favorites,{onPlay(c)},{onFavorite(c)})}}
    }
}

@Composable private fun ChannelRow(c:Channel,favorite:Boolean,onPlay:()->Unit,onFavorite:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).clickable{onPlay()}.padding(10.dp),Alignment.CenterVertically){
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF263144)),Alignment.Center){Text(c.name.take(2).uppercase(),Color.White,fontWeight=FontWeight.Bold)}
        Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(c.name,Color.White,FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(c.group.ifBlank{"ТВ канал"},Muted,12.sp);Spacer(Modifier.height(5.dp));Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A313C))){Box(Modifier.fillMaxWidth(.55f).height(2.dp).background(Purple))}}
        Text(if(favorite)"★" else "☆",if(favorite)Purple else Muted,25.sp,modifier=Modifier.clickable{onFavorite()}.padding(4.dp));Text("▶",Purple,16.sp,modifier=Modifier.padding(6.dp))
    }
}

@Composable private fun MoviePage(movies:List<Movie>,search:String,onSearch:(String)->Unit){
    val filtered=movies.filter{it.title.contains(search,true)||it.description.contains(search,true)}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        OutlinedTextField(search,onSearch,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Поиск фильмов",color=Muted)},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Card2,focusedTextColor=Color.White,unfocusedTextColor=Color.White,cursorColor=Purple))
        Spacer(Modifier.height(14.dp));Text("🎬 Доступное кино",Color.White,20.sp,FontWeight.Bold);Text("Public Domain / Creative Commons",Muted,12.sp,modifier=Modifier.padding(top=4.dp,bottom=12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=80.dp)){items(filtered){m->MovieCard(m)}}
    }
}

@Composable private fun MovieCard(m:Movie){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card).padding(10.dp),Alignment.CenterVertically){
        Box(Modifier.width(88.dp).height(118.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF302044),Color(0xFF171D29)))),Alignment.Center){Text("🎬",fontSize=32.sp)}
        Column(Modifier.weight(1f).padding(start=14.dp)){Text(m.title,Color.White,16.sp,FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text(m.year,Purple,12.sp,modifier=Modifier.padding(top=4.dp));Text(m.description,Muted,12.sp,maxLines=3,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp));Text("▶ Смотреть",Purple,13.sp,FontWeight.Bold,modifier=Modifier.padding(top=8.dp))}
    }
}

@Composable private fun BottomBar(page:String,onPage:(String)->Unit){
    NavigationBar(containerColor=Color(0xFF0D1117)){listOf("home" to "⌂\nГлавная","channels" to "▣\nТВ каналы","movies" to "🎬\nФильмы","favorites" to "♡\nИзбранное","settings" to "•••\nЕще").forEach{(id,label)->NavigationBarItem(page==id,{onPage(id)},{Text(label,textAlign=androidx.compose.ui.text.style.TextAlign.Center,fontSize=10.sp)},label=null)}}
}

@Composable private fun Drawer(onClose:()->Unit,onNavigate:(String)->Unit){
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.65f)).clickable{onClose()}){Column(Modifier.fillMaxHeight().width(300.dp).background(Color(0xFF0D131A)).padding(24.dp).clickable(enabled=false){}){Spacer(Modifier.height(18.dp));Text("IPTV",Purple,28.sp,FontWeight.Black);Text("Player",Color.White,22.sp);Text("v0.1",Muted,12.sp,modifier=Modifier.padding(bottom=28.dp));listOf("home" to "⌂   Главная","channels" to "▣   ТВ каналы","movies" to "🎬   Фильмы","favorites" to "☆   Избранное","settings" to "⚙   Настройки").forEach{item->Text(item.second,Color.White,16.sp,modifier=Modifier.fillMaxWidth().clickable{onNavigate(item.first)}.padding(vertical=15.dp,horizontal=12.dp))}}}
}

@Composable private fun SettingsPage(){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){SettingSection("ПЛЕЕР",listOf("Плеер по умолчанию" to "ExoPlayer","Аппаратное декодирование" to "Вкл","Автозапуск следующего канала" to "Выкл"));SettingSection("ПРИЛОЖЕНИЕ",listOf("Язык" to "Русский","Тема" to "Темная"));SettingSection("ПЛЕЙЛИСТ",listOf("Источник" to "M3U","Обновлять при запуске" to "Вкл"));Text("EPG отключен",Muted,12.sp)}}
@Composable private fun SettingSection(title:String,values:List<Pair<String,String>>){Text(title,Muted,11.sp,FontWeight.Bold);Card(colors=CardDefaults.cardColors(containerColor=Card),shape=RoundedCornerShape(15.dp)){Column{values.forEachIndexed{i,p->Row(Modifier.fillMaxWidth().padding(16.dp,15.dp),Alignment.CenterVertically){Text(p.first,Color.White,modifier=Modifier.weight(1f));Text(p.second,Muted);Text("›",Muted,22.sp,modifier=Modifier.padding(start=8.dp))};if(i<values.lastIndex)HorizontalDivider(color=Color(0xFF222A35))}}}}

@Composable private fun PlayerScreen(c:Channel,onBack:()->Unit){
    val context=LocalContext.current;val player=remember(c.url){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(c.url));prepare();playWhenReady=true}};DisposableEffect(player){onDispose{player.release()}}
    Column(Modifier.fillMaxSize().background(Color.Black)){Row(Modifier.fillMaxWidth().padding(10.dp),Alignment.CenterVertically){Text("‹",Color.White,34.sp,modifier=Modifier.clickable{onBack()});Column(Modifier.weight(1f).padding(start=8.dp)){Text(c.name,Color.White,FontWeight.Bold);Text(c.group,Muted,12.sp)};Text("☆",Color.White,28.sp)};AndroidView(factory={ctx->PlayerView(ctx).apply{this.player=player;useController=true;controllerAutoShow=true}},modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f));Text("● LIVE   Прямой эфир",Color(0xFFFF4B4B),modifier=Modifier.padding(18.dp))}
}

private fun loadM3u(url:String):List<Channel>=try{parseM3u(URL(url).readText())}catch(_:Exception){emptyList()}
private fun parseM3u(text:String):List<Channel>{val result=mutableListOf<Channel>();var name="";var group="";var logo:String?=null;var ua:String?=null;text.lineSequence().forEach{raw->val line=raw.trim();when{line.startsWith("#EXTINF:",true)->{name=line.substringAfterLast(",").trim();group=Regex("""group-title=\"([^\"]*)\"""",RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty();logo=Regex("""tvg-logo=\"([^\"]*)\"""",RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)};line.startsWith("#EXTVLCOPT:http-user-agent=",true)->ua=line.substringAfter("=","").trim();line.startsWith("#")||line.isBlank()->Unit;else->{if(name.isNotBlank())result+=Channel(name,group,logo,line,ua);name="";group="";logo=null;ua=null}}};return result}

private fun legalMovies():List<Movie>=listOf(
    Movie("Night of the Living Dead","1968","Классический фильм, доступный в коллекциях public domain."),
    Movie("His Girl Friday","1940","Классическая романтическая комедия из старой киноколлекции."),
    Movie("Charade","1963","Классический детективный фильм; права необходимо проверять по территории."),
    Movie("The General","1926","Немая классика Бастера Китона, общественное достояние."),
    Movie("The Cabinet of Dr. Caligari","1920","Классика немецкого экспрессионизма, общественное достояние.")
)
