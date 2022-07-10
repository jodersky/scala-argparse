-- Prepend a path to all links in the document.
-- The path must be given as a metadata variabe named "prefix".

local prefix = ""

local function starts_with(str, start)
  return str:sub(1, #start) == start
end

function transform_meta(meta)
  prefix = meta["prefix"]
  return meta
end

function transform_link(el)
  if not starts_with(el.target, "http") then
    el.target = prefix.."/"..el.target
  end
  return el
end

function transform_image(el)
  if not starts_with(el.src, "http") then
    el.src = prefix.."/"..el.src
  end
  return el
end

return {{Meta = transform_meta}, {Link = transform_link}, {Image = transform_image}}
