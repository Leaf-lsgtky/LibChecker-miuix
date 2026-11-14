package com.absinthe.libchecker.domain.about.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GitHubContributorResp
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.about.model.CachedDeveloperInfo
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.ui.compose.component.LcLoadingState
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.utils.SPUtils
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.squareup.moshi.Types
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/** Contributors sheet of the about page, backed by the GitHub contributors API. */
@Composable
fun DevelopersSheet(
  show: Boolean,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var items by remember { mutableStateOf(FALLBACK_ITEMS) }
  var loading by remember { mutableStateOf(false) }

  LaunchedEffect(show) {
    if (!show) return@LaunchedEffect
    val tokenCacheKey = GlobalValues.githubApiTokenVersion.toString()
    val cached = readCachedItems()
    if (cached != null) {
      items = cached.items
      if (!cached.shouldRefresh(tokenCacheKey)) return@LaunchedEffect
    }
    loading = true
    val authorization = GlobalValues.githubApiAuthorizationHeaderFor(
      ApiManager.GITHUB_API_REPO_CONTRIBUTORS
    )
    val loaded = runCatching { loadGitHubContributors(authorization) }
      .onFailure { Timber.w(it, "Failed to load GitHub contributors") }
      .getOrNull()
    if (!loaded.isNullOrEmpty()) {
      items = loaded
      cacheItems(loaded, tokenCacheKey)
    }
    loading = false
  }

  WindowBottomSheet(
    show = show,
    title = stringResource(R.string.about_developers),
    onDismissRequest = onDismiss
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
    ) {
      if (loading) {
        LcLoadingState(
          modifier = Modifier.height(120.dp),
          message = stringResource(R.string.loading)
        )
      } else {
        LazyColumn(
          modifier = Modifier
            .heightIn(max = 480.dp)
            .padding(top = 8.dp)
        ) {
          items(items, key = { it.github + it.name }) { developer ->
            DeveloperRow(
              developer = developer,
              onClick = { context.openUrlInBrowser(developer.github) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeveloperRow(developer: DeveloperInfo, onClick: () -> Unit) {
  val colorScheme = MiuixTheme.colorScheme
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = developer.avatarUrl,
      contentDescription = null,
      modifier = Modifier
        .padding(end = 12.dp)
        .size(40.dp)
        .clip(CircleShape)
    )
    Column(Modifier.weight(1f)) {
      Text(
        text = developer.name,
        fontSize = 14.sp,
        fontWeight = FontWeight(550),
        color = colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = developer.desc,
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

private suspend fun loadGitHubContributors(authorization: String?): List<DeveloperInfo> {
  val request = ApiManager.create<CloudRuleBundleRequest>()
  return runCatching {
    request.loadContributorDeveloperInfo(authorization)
  }.recoverCatching { error ->
    if (authorization != null && error.isUnauthorized()) {
      request.loadContributorDeveloperInfo(null)
    } else {
      throw error
    }
  }.getOrThrow()
}

private suspend fun CloudRuleBundleRequest.loadContributorDeveloperInfo(
  authorization: String?
): List<DeveloperInfo> {
  return getAllContributors(authorization)
    .filterNot { it.isBot() }
    .mergeContributorAliases()
    .sortedByDescending(GitHubContributorResp::contributions)
    .mapNotNull { it.toDeveloperInfo() }
}

private suspend fun CloudRuleBundleRequest.getAllContributors(
  authorization: String?
): List<GitHubContributorResp> {
  val contributors = mutableListOf<GitHubContributorResp>()
  var page = 1
  do {
    val pageItems = withContext(Dispatchers.IO) {
      requestContributors(
        owner = GITHUB_OWNER,
        repo = GITHUB_REPO,
        authorization = authorization,
        page = page
      )
    }
    contributors += pageItems
    page++
  } while (pageItems.size == GITHUB_PAGE_SIZE)
  return contributors
}

private fun Throwable.isUnauthorized(): Boolean {
  return this is HttpException && (code() == 401 || code() == 403)
}

private fun GitHubContributorResp.isBot(): Boolean {
  val fields = listOfNotNull(login, name, email).map { it.lowercase(Locale.US) }
  return type.equals("Bot", ignoreCase = true) ||
    fields.any { it.endsWith("[bot]") || it.endsWith("-bot") || it.endsWith("bot") }
}

private fun List<GitHubContributorResp>.mergeContributorAliases(): List<GitHubContributorResp> {
  val merged = linkedMapOf<String, GitHubContributorResp>()
  forEach { contributor ->
    val key = contributor.identityKey() ?: return@forEach
    val existing = merged[key]
    merged[key] = existing?.mergeWith(contributor) ?: contributor
  }
  return merged.values.toList()
}

private fun GitHubContributorResp.identityKey(): String? {
  return (login?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() })
    ?.lowercase(Locale.US)
}

private fun GitHubContributorResp.mergeWith(other: GitHubContributorResp): GitHubContributorResp {
  val preferredProfile = if (other.profileScore() > profileScore()) other else this
  val fallback = if (preferredProfile === this) other else this
  return preferredProfile.copy(
    name = preferredProfile.name ?: fallback.name,
    email = preferredProfile.email ?: fallback.email,
    contributions = contributions + other.contributions
  )
}

private fun GitHubContributorResp.profileScore(): Int {
  var score = 0
  if (!login.isNullOrBlank() && !htmlUrl.isNullOrBlank()) {
    score += 2
  }
  if (!avatarUrl.isNullOrBlank()) {
    score += 1
  }
  return score
}

private fun GitHubContributorResp.toDescription(): String {
  val role = if (isMaintainer()) "Maintainer" else "Contributor"
  val commitText = if (contributions == 1) "1 commit" else "$contributions commits"
  return "$role, $commitText"
}

private fun GitHubContributorResp.toDeveloperInfo(): DeveloperInfo? {
  val displayName = login?.takeIf { it.isNotBlank() }
    ?: name?.takeIf { it.isNotBlank() }
    ?: return null
  val githubUrl = htmlUrl?.takeIf { it.isNotBlank() }
    ?: login?.takeIf { it.isNotBlank() }?.let { "https://github.com/$it" }
    ?: URLManager.GITHUB_REPO_PAGE
  val avatar = avatarUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_AVATAR_URL
  return DeveloperInfo(
    name = displayName,
    desc = toDescription(),
    github = githubUrl,
    avatarUrl = avatar
  )
}

private fun GitHubContributorResp.isMaintainer(): Boolean {
  return login.equals(MAINTAINER_LOGIN, ignoreCase = true) ||
    htmlUrl?.substringAfterLast("/")?.equals(MAINTAINER_LOGIN, ignoreCase = true) == true
}

private fun DeveloperInfo.toCachedDeveloperInfo(): CachedDeveloperInfo {
  return CachedDeveloperInfo(name = name, desc = desc, github = github, avatarUrl = avatarUrl)
}

private fun CachedDeveloperInfo.toDeveloperInfo(): DeveloperInfo {
  return DeveloperInfo(name = name, desc = desc, github = github, avatarUrl = avatarUrl)
}

private fun readCachedItems(): DeveloperInfoCache? {
  val cacheVersion = SPUtils.getValue(PREF_DEVELOPERS_CACHE_VERSION, 0)
  if (cacheVersion != DEVELOPERS_CACHE_VERSION) {
    return null
  }
  val json = SPUtils.getValue(PREF_DEVELOPERS_CACHE, "")
  if (json.isEmpty()) {
    return null
  }
  val items = runCatching { developerCacheAdapter.fromJson(json) }
    .getOrNull()
    ?.map { it.toDeveloperInfo() }
    ?.takeIf { it.isNotEmpty() }
    ?: return null
  val cachedAt = SPUtils.getValue(PREF_DEVELOPERS_CACHE_TIMESTAMP, 0L)
  val tokenCacheKey = SPUtils.getValue(PREF_DEVELOPERS_CACHE_TOKEN_KEY, "")
  return DeveloperInfoCache(items, cachedAt, tokenCacheKey)
}

private fun cacheItems(items: List<DeveloperInfo>, tokenCacheKey: String) {
  val json = developerCacheAdapter.toJson(items.map { it.toCachedDeveloperInfo() })
  SPUtils.putValue(PREF_DEVELOPERS_CACHE, json)
  SPUtils.putValue(PREF_DEVELOPERS_CACHE_TIMESTAMP, System.currentTimeMillis())
  SPUtils.putValue(PREF_DEVELOPERS_CACHE_TOKEN_KEY, tokenCacheKey)
  SPUtils.putValue(PREF_DEVELOPERS_CACHE_VERSION, DEVELOPERS_CACHE_VERSION)
}

private data class DeveloperInfoCache(
  val items: List<DeveloperInfo>,
  val cachedAt: Long,
  val tokenCacheKey: String
) {
  fun shouldRefresh(currentTokenCacheKey: String): Boolean {
    return cachedAt <= 0 ||
      System.currentTimeMillis() - cachedAt > DEVELOPERS_CACHE_VALID_DURATION ||
      tokenCacheKey != currentTokenCacheKey
  }
}

private const val GITHUB_OWNER = "LibChecker"
private const val GITHUB_REPO = "LibChecker"
private const val GITHUB_PAGE_SIZE = 100
private const val MAINTAINER_LOGIN = "zhaobozhen"
private const val DEFAULT_AVATAR_URL = "https://avatars.githubusercontent.com/u/116417672"
private const val PREF_DEVELOPERS_CACHE = "developers_cache"
private const val PREF_DEVELOPERS_CACHE_TIMESTAMP = "developers_cache_timestamp"
private const val PREF_DEVELOPERS_CACHE_TOKEN_KEY = "developers_cache_token_key"
private const val PREF_DEVELOPERS_CACHE_VERSION = "developers_cache_version"
private const val DEVELOPERS_CACHE_VERSION = 2
private const val DEVELOPERS_CACHE_VALID_DURATION = 24 * 60 * 60 * 1000L

private val developerCacheAdapter by lazy {
  JsonUtil.moshi.adapter<List<CachedDeveloperInfo>>(
    Types.newParameterizedType(List::class.java, CachedDeveloperInfo::class.java)
  )
}

private val FALLBACK_ITEMS = listOf(
  DeveloperInfo(
    "Absinthe",
    "Maintainer",
    "https://github.com/zhaobozhen",
    "https://avatars.githubusercontent.com/u/25247117"
  ),
  DeveloperInfo(
    "Goooler",
    "Code Tidy & Optimize",
    "https://github.com/Goooler",
    "https://avatars.githubusercontent.com/u/10363352"
  ),
  DeveloperInfo(
    "qhy040404",
    "Developer",
    "https://github.com/qhy040404",
    "https://avatars.githubusercontent.com/u/45379733"
  ),
  DeveloperInfo(
    "Source Code",
    URLManager.GITHUB_REPO_PAGE,
    URLManager.GITHUB_REPO_PAGE,
    "https://avatars.githubusercontent.com/u/116417672"
  )
)