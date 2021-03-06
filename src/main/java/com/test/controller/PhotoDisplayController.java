package com.test.controller;

import com.test.dataaccess.DatabaseAccess;
import com.test.entity.PhotoCategory;
import com.test.entity.Photos;
import com.test.external.FBConnection;
import com.test.external.GoogleMapsAPI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * This class controller is for displaying photos in the browse and picture-details pages.
 */
@Controller
public class PhotoDisplayController {

    // Class fields
    private int NUM_PHOTO_PER_PAGE = 3;  // number of photos per page


    /**
     * This method will show the browse page for the appropriate category
     * @param category String category of the browse
     * @param currentPage int of current page that user is browsing
     * @param model Model, if needed
     * @return String browse page
     */
    @RequestMapping(value="browse")
    public String showBrowsePage(@RequestParam("cat") PhotoCategory category,
                                 @RequestParam("page") int currentPage,
                                 HttpServletRequest request,
                                 Model model) {

        // Logging
        System.out.println("PhotoDisplayController.showBrowsePage: Showing category " + category + " browse page");

        // Get total number of photos for a category
        int numEntries = DatabaseAccess.getNumberOfEntries(category);

        // If no entries, show empty page
        if (numEntries < 1) {
            System.out.println("Error! No entries in category: " + category.toString());
            model.addAttribute("message", "No photo entries in category - " + category.toString() + ".  " +
                    "Be the first one to upload! <a href=\"submit\"");
            return "error";
        }

        // Determine number of pages needed
        int numPages;
        if (numEntries % NUM_PHOTO_PER_PAGE != 0) {
            numPages = numEntries / NUM_PHOTO_PER_PAGE + 1;
        }
        else {
            numPages = numEntries / NUM_PHOTO_PER_PAGE;
        }
        model.addAttribute("numPages", numPages);

        // Make sure requested page value is valid
        if (currentPage < 1) {
            currentPage = 1;
        }
        else if (currentPage > numPages) {
            currentPage = numPages;
        }

        // create Photo list to add to model depending on current page
        List<Photos> photos = null;  // Initialize
        if ( // This if statement is for displaying pages with NUM_PHOTO_PER_PAGE photos
                (currentPage == numPages && numEntries % NUM_PHOTO_PER_PAGE == 0)
                || (numPages >= 1 && currentPage < numPages))
        {
            photos = DatabaseAccess
                    .getTopPhotos((currentPage-1)*NUM_PHOTO_PER_PAGE,
                            (currentPage-1)*NUM_PHOTO_PER_PAGE+(NUM_PHOTO_PER_PAGE-1),
                            category);
        }
        else if ( // This if statement is for the last page if there are less than NUM_PHOTO_PER_PAGE photos
                currentPage == numPages && numEntries % NUM_PHOTO_PER_PAGE != 0)
        {
            photos = DatabaseAccess
                    .getTopPhotos((currentPage-1)*NUM_PHOTO_PER_PAGE,
                            (currentPage-1)*NUM_PHOTO_PER_PAGE+(NUM_PHOTO_PER_PAGE-1),
                            category);
        }
        else { // You shouldn't get here...
            System.out.println("Error! Creating list of Photos failed!");
            model.addAttribute("message", "Couldn't retrieve photos from database.  Please try again later.");
            return "error";
        }

        // Add the photos list to attribute
        model.addAttribute("photos", photos);
        model.addAttribute("imageURL",
                request.getScheme() + "://" +
                        request.getServerName() + ":" +
                        request.getServerPort() + "/images/");

        // Add current page information
        model.addAttribute("currentPage", currentPage);

        // Add number of pages
        model.addAttribute("numPages", numPages);

        // Add the current category
        model.addAttribute("category", category);

        // Show the browse page to user
        return "browse";
    }


    /**
     * This method is going to show the detailed view of the image with upvote/downvote, and map view
     * @param photoID int PhotoID of the photo to be displayed
     * @param request HttpServletRequest to get the local machine URL
     * @param model Model to add attributes to display on page
     * @return String photo-detail page
     */
    @RequestMapping("photo")
    public String showPhotoDetails(@RequestParam("id") int photoID,
                                   @RequestParam("prev") int prevPage,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   Model model) {

        // Pass back the current page for "back to browse"
        model.addAttribute("prevPage", prevPage);

        // Get photo and add to model
        Photos photo = DatabaseAccess.getPhoto(photoID);
        model.addAttribute("photo", photo);

        // Get URL of images and add to model
        model.addAttribute("imageURL",
                request.getScheme() + "://" +
                        request.getServerName() + ":" +
                        request.getServerPort() + "/images/");

        // Add vote status to the model to color the buttons
        if (LoginController.userLoggedIn(session)) {
            model.addAttribute("voteStat",
                    VoteController.userLastVote(session.getAttribute("userID").toString(), photoID));
        }
        else {
            model.addAttribute("voteStat", 0);
        }

        // Add category to attribute, to allow going back to home page
        model.addAttribute("category", PhotoCategory.getEnum(photo.getCategory()));

        // Get API Key for Google Maps
        model.addAttribute("apiKey", GoogleMapsAPI.getApiKey());

        // Return facebook login url
        model.addAttribute("facebookLogin", FBConnection.getFBAuthUrl(
                request.getScheme() + "://" +
                        request.getServerName() + ":" +
                        request.getServerPort() + "/"));

        return "photo-detail";
    }

}