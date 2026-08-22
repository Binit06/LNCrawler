"""[Feature]: Redirect to crawler site when clicked on the sources (fix for issue #31)"""

def handle_source_click(source_url):
    if source_url.endswith(('.html', '.htm')):
        redirect_url = source_url.rsplit('/', 1)[0]
        if redirect_url.endswith('/'):
            redirect_url = redirect_url[:-1]
        if redirect_url:
            return f"<meta http-equiv='refresh' content='0; url={redirect_url}'>"
    return None

class CrawlerSiteRedirect:
    def __init__(self):
        self.redirect_map = {}

    def add_redirect(self, source_url, target_url):
        self.redirect_map[source_url] = target_url

    def resolve_redirect(self, source_url):
        if source_url in self.redirect_map:
            return f"<meta http-equiv='refresh' content='0; url={self.redirect_map[source_url]}'>"
        prefix = source_url.rsplit('/', 1)[0]
        if prefix.endswith(('.html', '.htm')):
            return f"<meta http-equiv='refresh' content='0; url={prefix}\n'>"
        return None

# Example usage:
# redirector = CrawlerSiteRedirect()
# redirector.add_redirect('https://example.com/source.html', 'https://example.com/main')
# print(redirector.resolve_redirect('https://example.com/source.html'))

