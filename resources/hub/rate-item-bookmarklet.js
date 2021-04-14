javascript:(function () {
  let form_url = "https://hub.findka.com/hub.curate/rate/";

  let q = (s) => document.querySelector(s);
  let get = (m, k) => m ? m[k] : null;

  let title = get(q('title'), 'innerHTML') || "";
  let description = get(q('meta[name=description]'), 'content') ||
      get(q("meta[property='og:description']"), 'content') ||
      get(q("meta[property='twitter:description']"), 'content') ||
      "";
  let image = get(q('meta[name=image]'), 'content') ||
      get(q("meta[property='og:image']"), 'content') ||
      get(q("meta[property='twitter:image']"), 'content') ||
      "";
  var feed = get(q("link[type='application/atom+xml']"), 'href') ||
      get(q("link[type='application/rss+xml']"), 'href') ||
      "";
  feed = (new URL(feed, document.location)).href;

  window.location = airtable_form_url +
      "?url=" + encodeURIComponent(document.location) +
      "&title=" + encodeURIComponent(title) +
      "&description=" + encodeURIComponent(description) +
      "&image=" + encodeURIComponent(image) +
      "&feed=" + encodeURIComponent(feed);
})()
