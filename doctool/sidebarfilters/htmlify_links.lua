-- Change any links targeting markdown files to point to their equivalent html
-- document.

local function starts_with(str, start)
  return str:sub(1, #start) == start
end

local function ends_with(str, ending)
  return ending == "" or str:sub(-#ending) == ending
end


function Link(el)
  -- We assume that absolute links are external and should not be modified
  if starts_with(el.target, "http") then
    return el
  end
  
  el.target = string.gsub(el.target, "%.md", ".html")
  
  -- if not ends_with(el.target, ".html") then
  --   el.target = el.target..".html"
  -- end

  return el
end
