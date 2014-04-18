# The DocPad Configuration File
# It is simply a CoffeeScript Object which is parsed by CSON
docpadConfig =

  # Template Data
  # =============
  # These are variables that will be accessible via our templates
  # To access one of these within our templates, refer to the FAQ: https://github.com/bevry/docpad/wiki/FAQ

  templateData:

    # Specify some site properties
    site:
      # The production url of our website
      url: "http://sookocheff.com"

      # The default title of our website
      title: "Kevin Sookocheff"

      # The website description (for SEO)
      description: """Tinker. Tailor. Soldier. Sailor."""

      # The website author's name
      author: "Kevin Sookocheff"

      # The website author's email
      email: "kevin.sookocheff@gmail.com"

      # Your company's name
      copyright: "© Kevin Sookocheff"


    # Helper Functions
    # ----------------

    # Get the prepared site/document title
    # Often we would like to specify particular formatting to our page's title
    # we can apply that formatting here
    getPreparedTitle: ->
      # if we have a document title, then we should use that and suffix the site's title onto it
      if @document.title
        "#{@document.title} | #{@site.title}"
      # if our document does not have it's own title, then we should just use the site's title
      else
        @site.title

    # Get the prepared site/document description
    getPreparedDescription: ->
      # if we have a document description, then we should use that, otherwise use the site's description
      @document.description or @site.description

    # Get the prepared site/document keywords
    getPreparedKeywords: ->
      # Merge the document keywords with the site keywords
      @site.keywords.concat(@document.keywords or []).join(', ')

  # Collections
  # ===========
  # These are special collections that our website makes available to us

  collections:
    # For instance, this one will fetch in all documents that have pageOrder set within their meta data
    pages: (database) ->
      database.findAllLive({pageOrder: $exists: true}, [pageOrder:1,title:1])

    # This one, will fetch in all documents that will be outputted to the posts directory
    posts: ->
        @getCollection('html').findAllLive(
                relativeOutDirPath: 'posts'
                isPagedAuto: $ne: true,
                {date:-1}
            )

  plugins:
    rss: {
        "default": {
            "collection": "posts",
            "url": "/rss.xml"
        }
     }
    dateurls:
      cleanurl: true
      trailingSlashes: true
    ghpages:
        deployRemote: 'target'
        deployBranch: 'master'


# Export our DocPad Configuration
module.exports = docpadConfig
