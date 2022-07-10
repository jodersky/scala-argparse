-- Adds anchor links to headers, suitable to be by styled with css.
-- E.g. the html header:
--   <h1 id="example-header">Example Header</h1>
-- becomes
--   <h1 id="example-header">Example Header<a class="anchor" href="#example-header"></h1>

function Header(el)
    local attrs = pandoc.Attr("", {"anchor"})
    local link = pandoc.Link("", "#"..el.attr.identifier, "", attrs)
    table.insert(el.content, link)
    return pandoc.Header(el.level, el.content, el.attr);
end
